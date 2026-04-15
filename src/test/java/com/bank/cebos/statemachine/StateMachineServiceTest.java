package com.bank.cebos.statemachine;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.EmployeeStatusHistory;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateMachineServiceTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  @Captor private ArgumentCaptor<EmployeeStatusHistory> historyCaptor;

  private StateMachineService stateMachineService;

  @BeforeEach
  void setUp() {
    stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    lenient()
        .when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(999L);
              }
              return e;
            });
  }

  private static EmployeeOnboarding newOnboarding() {
    try {
      Constructor<EmployeeOnboarding> c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void legalOtpVerifiedToOcrPersistsEmployeeAndHistory() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setId(10L);
    employee.setEmployeeRef("E-1");
    employee.setStatus(OnboardingStatus.OTP_VERIFIED);

    stateMachineService.transition(employee, OnboardingStatus.OCR_IN_PROGRESS, "system", "ocr start");

    verify(employeeOnboardingRepository).save(employee);
    verify(employeeStatusHistoryRepository).save(historyCaptor.capture());
    EmployeeStatusHistory h = historyCaptor.getValue();
    org.assertj.core.api.Assertions.assertThat(h.getFromStatus()).isEqualTo("OTP_VERIFIED");
    org.assertj.core.api.Assertions.assertThat(h.getToStatus()).isEqualTo("OCR_IN_PROGRESS");
    org.assertj.core.api.Assertions.assertThat(h.getChangedBy()).isEqualTo("system");
    org.assertj.core.api.Assertions.assertThat(employee.getStatus()).isEqualTo(OnboardingStatus.OCR_IN_PROGRESS);
  }

  @Test
  void illegalSkipDoesNotPersist() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setId(11L);
    employee.setEmployeeRef("E-2");
    employee.setStatus(OnboardingStatus.OTP_VERIFIED);

    assertThatThrownBy(
            () ->
                stateMachineService.transition(
                    employee, OnboardingStatus.FORM_PENDING, "system", "skip"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Illegal onboarding transition");

    verify(employeeOnboardingRepository, never()).save(any());
    verify(employeeStatusHistoryRepository, never()).save(any());
  }

  @Test
  void terminalAccountOpenedRejectsTransition() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setId(12L);
    employee.setEmployeeRef("E-3");
    employee.setStatus(OnboardingStatus.ACCOUNT_OPENED);

    assertThatThrownBy(
            () ->
                stateMachineService.transition(
                    employee, OnboardingStatus.OCR_IN_PROGRESS, "system", "replay"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("terminal");

    verify(employeeOnboardingRepository, never()).save(any());
    verify(employeeStatusHistoryRepository, never()).save(any());
  }

  @Test
  void persistNewEmployeeOnboardingWritesHistoryWithNullFromStatus() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setEmployeeRef("EMP-NEW-1");
    employee.setBatchId(1L);
    employee.setCorporateClientId(42L);
    employee.setStatus(OnboardingStatus.UPLOADED);

    stateMachineService.persistNewEmployeeOnboarding(
        employee, "uploader:7", "batch upload BATCH-x");

    org.assertj.core.api.Assertions.assertThat(employee.getId()).isEqualTo(999L);
    verify(employeeOnboardingRepository).save(employee);
    verify(employeeStatusHistoryRepository).save(historyCaptor.capture());
    EmployeeStatusHistory h = historyCaptor.getValue();
    org.assertj.core.api.Assertions.assertThat(h.getFromStatus()).isNull();
    org.assertj.core.api.Assertions.assertThat(h.getToStatus()).isEqualTo("UPLOADED");
    org.assertj.core.api.Assertions.assertThat(h.getChangedBy()).isEqualTo("uploader:7");
    org.assertj.core.api.Assertions.assertThat(h.getReason()).isEqualTo("batch upload BATCH-x");
    org.assertj.core.api.Assertions.assertThat(h.getEmployeeOnboarding()).isSameAs(employee);
  }

  @Test
  void persistNewEmployeeOnboardingRejectsWhenIdPresent() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setId(1L);
    employee.setEmployeeRef("EMP-X");
    employee.setBatchId(1L);
    employee.setCorporateClientId(1L);
    employee.setStatus(OnboardingStatus.UPLOADED);

    assertThatThrownBy(
            () -> stateMachineService.persistNewEmployeeOnboarding(employee, "u", "r"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("id null");

    verify(employeeOnboardingRepository, never()).save(any());
    verify(employeeStatusHistoryRepository, never()).save(any());
  }

  @Test
  void persistNewEmployeeOnboardingRejectsWrongStatus() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setEmployeeRef("EMP-X");
    employee.setBatchId(1L);
    employee.setCorporateClientId(1L);
    employee.setStatus(OnboardingStatus.INVITED);

    assertThatThrownBy(
            () -> stateMachineService.persistNewEmployeeOnboarding(employee, "u", "r"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UPLOADED");

    verify(employeeOnboardingRepository, never()).save(any());
    verify(employeeStatusHistoryRepository, never()).save(any());
  }

  @Test
  void administrativeBlockedToInvitedPersists() {
    EmployeeOnboarding employee = newOnboarding();
    employee.setId(13L);
    employee.setEmployeeRef("E-4");
    employee.setStatus(OnboardingStatus.BLOCKED);

    stateMachineService.transition(
        employee, OnboardingStatus.INVITED, "admin-1", "unblock", true);

    verify(employeeOnboardingRepository).save(employee);
    verify(employeeStatusHistoryRepository).save(historyCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(historyCaptor.getValue().getToStatus()).isEqualTo("INVITED");
    org.assertj.core.api.Assertions.assertThat(employee.getStatus()).isEqualTo(OnboardingStatus.INVITED);
  }
}
