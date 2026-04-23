package com.bank.cebos.service.auth;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.dto.auth.MobileInitResponse;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.entity.AuthRefreshToken;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.AuthRefreshTokenRepository;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.OtpSessionRepository;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MobileAuthService {

  private final EmployeeOnboardingService employeeOnboardingService;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final OtpSessionRepository otpSessionRepository;
  private final OtpService otpService;
  private final AuthRefreshTokenRepository authRefreshTokenRepository;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenHasher refreshTokenHasher;
  private final JwtProperties jwtProperties;
  private final boolean otpEchoEnabled;

  public MobileAuthService(
      EmployeeOnboardingService employeeOnboardingService,
      EmployeeOnboardingRepository employeeOnboardingRepository,
      OtpSessionRepository otpSessionRepository,
      OtpService otpService,
      AuthRefreshTokenRepository authRefreshTokenRepository,
      JwtTokenService jwtTokenService,
      RefreshTokenHasher refreshTokenHasher,
      JwtProperties jwtProperties,
      @Value("${cebos.otp.echo-enabled:false}") boolean otpEchoEnabled) {
    this.employeeOnboardingService = employeeOnboardingService;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.otpSessionRepository = otpSessionRepository;
    this.otpService = otpService;
    this.authRefreshTokenRepository = authRefreshTokenRepository;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenHasher = refreshTokenHasher;
    this.jwtProperties = jwtProperties;
    this.otpEchoEnabled = otpEchoEnabled;
  }

  @Transactional
  public MobileInitResponse initByMobile(String mobile) {
    String normalizedMobile = mobile == null ? "" : mobile.trim();
    EmployeeOnboarding employee =
        employeeOnboardingRepository
            .findByMobileAndStatus(normalizedMobile, OnboardingStatus.INVITED)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No pending invitation found for this mobile number"));

    String maskedMobile = maskMobile(employee.getMobile());
    String issuedOtp = otpService.issueOtp(employee.getId(), maskedMobile);
    String otpEcho = null;
    if (otpEchoEnabled) {
      otpSessionRepository
          .findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(employee.getId())
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.INTERNAL_SERVER_ERROR, "OTP session not created"));
      otpEcho = issuedOtp;
    }

    return new MobileInitResponse(
        employee.getId(), employee.getEmployeeRef(), maskedMobile, otpEcho);
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) {
    String hash = refreshTokenHasher.sha256Hex(request.refreshToken());
    AuthRefreshToken row =
        authRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    validateRefreshRow(row, PrincipalKind.MOBILE);
    EmployeeOnboarding employee =
        employeeOnboardingService.requireByIdForMobileRefresh(row.getPrincipalId());
    row.setRevoked(true);
    authRefreshTokenRepository.save(row);
    return issueTokens(employee);
  }

  @Transactional
  public TokenResponse issueTokensForEmployeeId(Long employeeOnboardingId) {
    EmployeeOnboarding employee =
        employeeOnboardingService.requireByIdForMobileRefresh(employeeOnboardingId);
    return issueTokens(employee);
  }

  private void validateRefreshRow(AuthRefreshToken row, PrincipalKind expectedKind) {
    if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    if (row.getPrincipalKind() != expectedKind) {
      throw new BadCredentialsException("Invalid refresh token");
    }
  }

  private TokenResponse issueTokens(EmployeeOnboarding employee) {
    String access =
        jwtTokenService.issueAccessToken(
            PrincipalKind.MOBILE,
            employee.getId(),
            employee.getCorporateClientId(),
            "",
            "/api/v1/mobile");
    String refreshPlain = refreshTokenHasher.newOpaqueRefreshToken();
    String tokenHash = refreshTokenHasher.sha256Hex(refreshPlain);
    Instant expiresAt =
        Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS);
    authRefreshTokenRepository.save(
        new AuthRefreshToken(
            tokenHash,
            PrincipalKind.MOBILE,
            employee.getId(),
            employee.getCorporateClientId(),
            expiresAt));
    return new TokenResponse(access, refreshPlain);
  }

  private static String maskMobile(String mobile) {
    String raw = mobile == null ? "" : mobile.trim();
    if (raw.length() <= 6) {
      return raw;
    }
    String prefix = raw.substring(0, 4);
    String suffix = raw.substring(raw.length() - 2);
    return prefix + "*".repeat(raw.length() - 6) + suffix;
  }
}
