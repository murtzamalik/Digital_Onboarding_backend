package com.bank.cebos.service.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.EmployeeStatusHistory;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeOnboardingServiceTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  private StateMachineService stateMachineService;
  private EmployeeOnboardingService employeeOnboardingService;

  @BeforeEach
  void setUp() {
    stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    employeeOnboardingService =
        new EmployeeOnboardingService(employeeOnboardingRepository, stateMachineService);
    when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(1000L);
              }
              return e;
            });
  }

  private static EmployeeOnboarding onboarding(long id, OnboardingStatus status) {
    try {
      var c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setId(id);
      e.setEmployeeRef("E-" + id);
      e.setStatus(status);
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void getRequiredByIdReturnsEntity() {
    EmployeeOnboarding row = onboarding(1L, OnboardingStatus.UPLOADED);
    when(employeeOnboardingRepository.findById(1L)).thenReturn(Optional.of(row));
    assertThat(employeeOnboardingService.getRequiredById(1L)).isSameAs(row);
  }

  @Test
  void getRequiredByIdMissingThrows404() {
    when(employeeOnboardingRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> employeeOnboardingService.getRequiredById(99L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void requireCurrentStatusRejectsWrongState() {
    EmployeeOnboarding row = onboarding(2L, OnboardingStatus.INVITED);
    assertThatThrownBy(
            () ->
                employeeOnboardingService.requireCurrentStatus(
                    row, OnboardingStatus.OTP_PENDING))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void requireForMobileOtpThrowsBadCredentialsWhenMissing() {
    when(employeeOnboardingRepository.findById(3L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> employeeOnboardingService.requireForMobileOtp(3L))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void transitionDelegatesToStateMachine() {
    EmployeeOnboarding row = onboarding(4L, OnboardingStatus.OTP_PENDING);
    ArgumentCaptor<EmployeeStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(EmployeeStatusHistory.class);
    employeeOnboardingService.transition(
        row, OnboardingStatus.OTP_VERIFIED, "u", "reason");
    verify(employeeOnboardingRepository).save(row);
    verify(employeeStatusHistoryRepository).save(historyCaptor.capture());
    assertThat(historyCaptor.getValue().getToStatus()).isEqualTo("OTP_VERIFIED");
    assertThat(row.getStatus()).isEqualTo(OnboardingStatus.OTP_VERIFIED);
  }
}
