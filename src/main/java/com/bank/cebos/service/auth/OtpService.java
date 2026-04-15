package com.bank.cebos.service.auth;

import com.bank.cebos.config.OtpProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.OtpSession;
import com.bank.cebos.enums.OtpSessionStatus;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.OtpSessionRepository;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.bank.cebos.util.PhoneE164;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);

  private static final String MOBILE_CHANNEL = "MOBILE";
  private static final String MAX_WRONG_ATTEMPTS_KEY = "otp.max.wrong.attempts";
  private static final String MAX_RESENDS_KEY = "otp.max.resends";

  private final OtpSessionRepository otpSessionRepository;
  private final EmployeeOnboardingService employeeOnboardingService;
  private final PasswordEncoder passwordEncoder;
  private final OtpCachePort otpCachePort;
  private final OtpProperties otpProperties;
  private final RuntimeConfigService runtimeConfigService;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;
  private final boolean otpEchoEnabled;
  private final SecureRandom secureRandom = new SecureRandom();

  public OtpService(
      OtpSessionRepository otpSessionRepository,
      EmployeeOnboardingService employeeOnboardingService,
      PasswordEncoder passwordEncoder,
      OtpCachePort otpCachePort,
      OtpProperties otpProperties,
      RuntimeConfigService runtimeConfigService,
      OutboxService outboxService,
      ObjectMapper objectMapper,
      @Value("${cebos.otp.echo-enabled:false}") boolean otpEchoEnabled) {
    this.otpSessionRepository = otpSessionRepository;
    this.employeeOnboardingService = employeeOnboardingService;
    this.passwordEncoder = passwordEncoder;
    this.otpCachePort = otpCachePort;
    this.otpProperties = otpProperties;
    this.runtimeConfigService = runtimeConfigService;
    this.outboxService = outboxService;
    this.objectMapper = objectMapper;
    this.otpEchoEnabled = otpEchoEnabled;
  }

  @Transactional
  public String issueOtp(Long employeeOnboardingId, String destinationMasked) {
    EmployeeOnboarding employee = employeeOnboardingService.requireForMobileOtp(employeeOnboardingId);
    if (employee.getStatus() == OnboardingStatus.INVITED) {
      employeeOnboardingService.transition(
          employee,
          OnboardingStatus.OTP_PENDING,
          "mobile:otp-issue",
          "Moving to OTP_PENDING for OTP issuance");
    }
    employeeOnboardingService.requireCurrentStatusForOtp(
        employee, EnumSet.of(OnboardingStatus.OTP_PENDING));
    String otp = generateOtp();
    String otpHash = passwordEncoder.encode(otp);
    Instant expiresAt = Instant.now().plus(otpProperties.expiryMinutes(), ChronoUnit.MINUTES);

    OtpSession session = new OtpSession();
    session.setEmployeeOnboardingId(employeeOnboardingId);
    session.setChannel(MOBILE_CHANNEL);
    session.setDestinationMasked(destinationMasked);
    session.setOtpHash(otpHash);
    session.setExpiresAt(expiresAt);
    session.setAttemptCount(0);
    session.setMaxAttempts(
        runtimeConfigService.getInt(MAX_WRONG_ATTEMPTS_KEY, otpProperties.maxAttempts()));
    session.setStatus(OtpSessionStatus.ACTIVE);
    otpSessionRepository.save(session);

    otpCachePort.writeHash(employeeOnboardingId, otpHash, Duration.between(Instant.now(), expiresAt));
    enqueueOtpSms(employee, otp);
    return otpEchoEnabled ? otp : null;
  }

  @Transactional
  public boolean verifyOtp(Long employeeOnboardingId, String otp) {
    EmployeeOnboarding employee = employeeOnboardingService.requireForMobileOtp(employeeOnboardingId);
    employeeOnboardingService.requireCurrentStatusForOtp(
        employee, EnumSet.of(OnboardingStatus.OTP_PENDING));
    OtpSession session = latestSession(employeeOnboardingId);
    validateSessionForVerification(session);

    String activeHash = resolveActiveHash(employeeOnboardingId, session);
    boolean matches = passwordEncoder.matches(otp, activeHash);
    if (!matches) {
      int newAttemptCount = session.getAttemptCount() + 1;
      session.setAttemptCount(newAttemptCount);
      if (newAttemptCount >= session.getMaxAttempts()) {
        session.setStatus(OtpSessionStatus.LOCKED);
      }
      otpSessionRepository.save(session);
      if (newAttemptCount >= session.getMaxAttempts()) {
        throw new BadCredentialsException("OTP attempts exceeded");
      }
      throw new BadCredentialsException("Invalid OTP");
    }

    session.setStatus(OtpSessionStatus.VERIFIED);
    session.setVerifiedAt(Instant.now());
    otpSessionRepository.save(session);
    otpCachePort.deleteHash(employeeOnboardingId);
    employeeOnboardingService.transition(
        employee,
        OnboardingStatus.OTP_VERIFIED,
        "mobile:otp-verify",
        "OTP verified");
    employeeOnboardingService.transition(
        employee,
        OnboardingStatus.OCR_IN_PROGRESS,
        "mobile:otp-verify",
        "OTP verified, moving to KYC OCR");
    return true;
  }

  @Transactional
  public String resendOtp(Long employeeOnboardingId) {
    EmployeeOnboarding employee = employeeOnboardingService.requireForMobileOtp(employeeOnboardingId);
    employeeOnboardingService.requireCurrentStatusForOtp(
        employee, EnumSet.of(OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING));
    OtpSession latest = latestSession(employeeOnboardingId);
    int resendWindowMinutes = otpProperties.resendWindowMinutes();
    int maxResends = runtimeConfigService.getInt(MAX_RESENDS_KEY, otpProperties.maxResends());
    Instant windowStart = Instant.now().minus(resendWindowMinutes, ChronoUnit.MINUTES);
    long sentInWindow =
        otpSessionRepository.countByEmployeeOnboardingIdAndCreatedAtAfter(employeeOnboardingId, windowStart);
    if (sentInWindow >= maxResends) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP resend limit exceeded");
    }
    return issueOtp(employeeOnboardingId, latest.getDestinationMasked());
  }

  private OtpSession latestSession(Long employeeOnboardingId) {
    return otpSessionRepository
        .findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(employeeOnboardingId)
        .orElseThrow(() -> new BadCredentialsException("OTP session not found"));
  }

  private void validateSessionForVerification(OtpSession session) {
    Instant now = Instant.now();
    if (session.getStatus() == OtpSessionStatus.LOCKED) {
      throw new BadCredentialsException("OTP attempts exceeded");
    }
    if (session.getVerifiedAt() != null) {
      throw new BadCredentialsException("OTP already verified");
    }
    if (session.getExpiresAt().isBefore(now)) {
      throw new BadCredentialsException("OTP expired");
    }
    if (session.getAttemptCount() >= session.getMaxAttempts()) {
      throw new BadCredentialsException("OTP attempts exceeded");
    }
  }

  private String resolveActiveHash(Long employeeOnboardingId, OtpSession session) {
    Optional<String> redisHash = otpCachePort.readHash(employeeOnboardingId);
    if (redisHash.isPresent() && !redisHash.get().isBlank()) {
      return redisHash.get();
    }
    return session.getOtpHash();
  }

  private void enqueueOtpSms(EmployeeOnboarding employee, String otpPlain) {
    String to = PhoneE164.toE164(employee.getMobile());
    if (to == null) {
      return;
    }
    try {
      Map<String, String> payload = new LinkedHashMap<>();
      payload.put("toE164", to);
      payload.put(
          "body",
          "CEBOS: your one-time code is "
              + otpPlain
              + ". Do not share this code. Ref "
              + employee.getEmployeeRef()
              + ".");
      outboxService.enqueue(
          OutboxEventType.SMS_SEND,
          "EmployeeOnboarding",
          String.valueOf(employee.getId()),
          objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize OTP SMS outbox payload for employee {}", employee.getId(), e);
    }
  }

  private String generateOtp() {
    int length = otpProperties.length();
    if (length <= 0) {
      throw new IllegalStateException("OTP length must be positive");
    }
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(secureRandom.nextInt(10));
    }
    return sb.toString();
  }
}
