package com.bank.cebos.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.OtpProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.OtpSession;
import com.bank.cebos.entity.SystemConfiguration;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.enums.OtpSessionStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.repository.OtpSessionRepository;
import com.bank.cebos.repository.SystemConfigurationRepository;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

  @Mock private OtpSessionRepository otpSessionRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private OtpCachePort otpCachePort;
  @Mock private SystemConfigurationRepository systemConfigurationRepository;
  @Mock private OutboxService outboxService;

  @Captor private ArgumentCaptor<OtpSession> otpSessionCaptor;

  private OtpService otpService;
  private RuntimeConfigService runtimeConfigService;

  @BeforeEach
  void setUp() {
    runtimeConfigService = new RuntimeConfigService(systemConfigurationRepository);
    StateMachineService stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(1000L);
              }
              return e;
            });
    EmployeeOnboardingService employeeOnboardingService =
        new EmployeeOnboardingService(employeeOnboardingRepository, stateMachineService);
    otpService =
        new OtpService(
            otpSessionRepository,
            employeeOnboardingService,
            passwordEncoder,
            otpCachePort,
            new OtpProperties(6, 5, 3, 3, 15),
            runtimeConfigService,
            outboxService,
            new ObjectMapper(),
            true);
  }

  private static EmployeeOnboarding newOnboarding(long id, OnboardingStatus status) {
    try {
      var constructor = EmployeeOnboarding.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      EmployeeOnboarding e = constructor.newInstance();
      e.setId(id);
      e.setEmployeeRef("E-" + id);
      e.setStatus(status);
      e.setMobile("03001234567");
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void issueOtpPersistsAndCachesOtpHashWhenOtpPending() {
    when(systemConfigurationRepository.findByConfigKey("otp.max.wrong.attempts"))
        .thenReturn(Optional.empty());
    when(employeeOnboardingRepository.findById(101L))
        .thenReturn(Optional.of(newOnboarding(101L, OnboardingStatus.OTP_PENDING)));
    when(passwordEncoder.encode(anyString())).thenReturn("hash-1");

    String otp = otpService.issueOtp(101L, "***1234");

    assertThat(otp).matches("\\d{6}");
    verify(otpSessionRepository).save(otpSessionCaptor.capture());
    OtpSession saved = otpSessionCaptor.getValue();
    assertThat(saved.getEmployeeOnboardingId()).isEqualTo(101L);
    assertThat(saved.getChannel()).isEqualTo("MOBILE");
    assertThat(saved.getDestinationMasked()).isEqualTo("***1234");
    assertThat(saved.getOtpHash()).isEqualTo("hash-1");
    verify(otpCachePort).writeHash(anyLong(), anyString(), any(Duration.class));
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(outboxService)
        .enqueue(
            eq(OutboxEventType.SMS_SEND),
            eq("EmployeeOnboarding"),
            eq("101"),
            payloadCaptor.capture());
    assertThat(payloadCaptor.getValue())
        .contains("+923001234567", "one-time code", otp);
  }

  @Test
  void issueOtpSkipsSmsOutboxWhenMobileNotNormalizable() {
    when(systemConfigurationRepository.findByConfigKey("otp.max.wrong.attempts"))
        .thenReturn(Optional.empty());
    EmployeeOnboarding employee = newOnboarding(888L, OnboardingStatus.OTP_PENDING);
    employee.setMobile("   ");
    when(employeeOnboardingRepository.findById(888L)).thenReturn(Optional.of(employee));
    when(passwordEncoder.encode(anyString())).thenReturn("hash-x");

    otpService.issueOtp(888L, "***1234");

    verify(otpSessionRepository).save(any());
    verify(outboxService, never()).enqueue(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void issueOtpTransitionsInvitedToOtpPendingBeforeIssuing() {
    when(systemConfigurationRepository.findByConfigKey("otp.max.wrong.attempts"))
        .thenReturn(Optional.empty());
    EmployeeOnboarding employee = newOnboarding(707L, OnboardingStatus.INVITED);
    when(employeeOnboardingRepository.findById(707L)).thenReturn(Optional.of(employee));
    when(passwordEncoder.encode(anyString())).thenReturn("hash-inv");

    otpService.issueOtp(707L, "***0000");

    assertThat(employee.getStatus()).isEqualTo(OnboardingStatus.OTP_PENDING);
    verify(employeeStatusHistoryRepository).save(any());
  }

  @Test
  void verifyOtpFallsBackToDbHashWhenRedisUnavailable() {
    EmployeeOnboarding employee = newOnboarding(202L, OnboardingStatus.OTP_PENDING);
    when(employeeOnboardingRepository.findById(202L)).thenReturn(Optional.of(employee));
    OtpSession session = new OtpSession();
    session.setEmployeeOnboardingId(202L);
    session.setOtpHash("db-hash");
    session.setExpiresAt(Instant.now().plusSeconds(60));
    session.setAttemptCount(0);
    session.setMaxAttempts(3);
    session.setStatus(OtpSessionStatus.ACTIVE);
    when(otpSessionRepository.findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(202L))
        .thenReturn(Optional.of(session));
    when(otpCachePort.readHash(202L)).thenReturn(Optional.empty());
    when(passwordEncoder.matches("123456", "db-hash")).thenReturn(true);

    boolean verified = otpService.verifyOtp(202L, "123456");

    assertThat(verified).isTrue();
    verify(otpSessionRepository).save(session);
    verify(otpCachePort).deleteHash(202L);
    assertThat(employee.getStatus()).isEqualTo(OnboardingStatus.OCR_IN_PROGRESS);
  }

  @Test
  void verifyOtpIncrementsAttemptsWhenOtpInvalid() {
    EmployeeOnboarding employee = newOnboarding(303L, OnboardingStatus.OTP_PENDING);
    when(employeeOnboardingRepository.findById(303L)).thenReturn(Optional.of(employee));
    OtpSession session = new OtpSession();
    session.setEmployeeOnboardingId(303L);
    session.setOtpHash("db-hash");
    session.setExpiresAt(Instant.now().plusSeconds(60));
    session.setAttemptCount(1);
    session.setMaxAttempts(3);
    session.setStatus(OtpSessionStatus.ACTIVE);
    when(otpSessionRepository.findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(303L))
        .thenReturn(Optional.of(session));
    when(otpCachePort.readHash(303L)).thenReturn(Optional.of("redis-hash"));
    when(passwordEncoder.matches("000000", "redis-hash")).thenReturn(false);

    assertThatThrownBy(() -> otpService.verifyOtp(303L, "000000"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid OTP");
    assertThat(session.getAttemptCount()).isEqualTo(2);
    verify(otpSessionRepository).save(session);
  }

  @Test
  void resendOtpThrowsWhenWindowLimitExceeded() {
    when(systemConfigurationRepository.findByConfigKey("otp.max.resends")).thenReturn(Optional.empty());
    when(employeeOnboardingRepository.findById(404L))
        .thenReturn(Optional.of(newOnboarding(404L, OnboardingStatus.OTP_PENDING)));
    OtpSession latest = new OtpSession();
    latest.setDestinationMasked("***9999");
    when(otpSessionRepository.findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(404L))
        .thenReturn(Optional.of(latest));
    when(otpSessionRepository.countByEmployeeOnboardingIdAndCreatedAtAfter(anyLong(), any(Instant.class)))
        .thenReturn(3L);

    assertThatThrownBy(() -> otpService.resendOtp(404L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("OTP resend limit exceeded");
  }

  @Test
  void verifyOtpLocksSessionAfterMaxWrongAttempts() {
    EmployeeOnboarding employee = newOnboarding(505L, OnboardingStatus.OTP_PENDING);
    when(employeeOnboardingRepository.findById(505L)).thenReturn(Optional.of(employee));
    OtpSession session = new OtpSession();
    session.setEmployeeOnboardingId(505L);
    session.setOtpHash("db-hash");
    session.setExpiresAt(Instant.now().plusSeconds(60));
    session.setAttemptCount(2);
    session.setMaxAttempts(3);
    session.setStatus(OtpSessionStatus.ACTIVE);
    when(otpSessionRepository.findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(505L))
        .thenReturn(Optional.of(session));
    when(otpCachePort.readHash(505L)).thenReturn(Optional.of("redis-hash"));
    when(passwordEncoder.matches("000000", "redis-hash")).thenReturn(false);

    assertThatThrownBy(() -> otpService.verifyOtp(505L, "000000"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("OTP attempts exceeded");
    assertThat(session.getAttemptCount()).isEqualTo(3);
    assertThat(session.getStatus()).isEqualTo(OtpSessionStatus.LOCKED);
    verify(otpSessionRepository).save(session);
  }

  @Test
  void issueOtpUsesRuntimeConfigOverrideForMaxAttempts() {
    when(employeeOnboardingRepository.findById(606L))
        .thenReturn(Optional.of(newOnboarding(606L, OnboardingStatus.OTP_PENDING)));
    SystemConfiguration config = new SystemConfiguration();
    config.setConfigKey("otp.max.wrong.attempts");
    config.setConfigValue("7");
    when(systemConfigurationRepository.findByConfigKey(eq("otp.max.wrong.attempts")))
        .thenReturn(Optional.of(config));
    when(passwordEncoder.encode(anyString())).thenReturn("hash-7");

    otpService.issueOtp(606L, "***7777");

    verify(otpSessionRepository).save(otpSessionCaptor.capture());
    assertThat(otpSessionCaptor.getValue().getMaxAttempts()).isEqualTo(7);
  }

  @Test
  void issueOtpDoesNotReturnOtpWhenEchoDisabled() {
    when(systemConfigurationRepository.findByConfigKey("otp.max.wrong.attempts"))
        .thenReturn(Optional.empty());
    when(employeeOnboardingRepository.findById(909L))
        .thenReturn(Optional.of(newOnboarding(909L, OnboardingStatus.OTP_PENDING)));
    when(passwordEncoder.encode(anyString())).thenReturn("hash-909");
    EmployeeOnboardingService employeeOnboardingService =
        new EmployeeOnboardingService(
            employeeOnboardingRepository,
            new StateMachineService(
                employeeOnboardingRepository,
                employeeStatusHistoryRepository,
                new OnboardingTransitionPolicy()));
    OtpService serviceNoEcho =
        new OtpService(
            otpSessionRepository,
            employeeOnboardingService,
            passwordEncoder,
            otpCachePort,
            new OtpProperties(6, 5, 3, 3, 15),
            runtimeConfigService,
            outboxService,
            new ObjectMapper(),
            false);

    String otp = serviceNoEcho.issueOtp(909L, "***0909");

    assertThat(otp).isNull();
    verify(otpSessionRepository).save(any());
  }
}
