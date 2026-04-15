package com.bank.cebos.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class T24RetryJobTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  private StateMachineService stateMachineService;
  private T24RetryJob t24RetryJob;

  @BeforeEach
  void setUp() {
    stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    JobsProperties jobsProperties = new JobsProperties();
    jobsProperties.getT24Retry().setEnabled(true);
    jobsProperties.setT24RetryAttemptsPlaceholder(5);
    jobsProperties.setT24RetryBatchSize(10);
    t24RetryJob = new T24RetryJob(employeeOnboardingRepository, stateMachineService, jobsProperties);
  }

  @Test
  void runTransitionsT24FailedToPendingWithinCap() {
    EmployeeOnboarding row = t24Failed("E-T24-1");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.T24_FAILED), any(Pageable.class)))
        .thenReturn(List.of(row));

    t24RetryJob.run();

    verify(employeeOnboardingRepository).save(row);
    verify(employeeStatusHistoryRepository).save(any());
    org.assertj.core.api.Assertions.assertThat(row.getStatus()).isEqualTo(OnboardingStatus.T24_PENDING);
  }

  @Test
  void runSkipsWhenDisabled() {
    JobsProperties disabled = new JobsProperties();
    disabled.getT24Retry().setEnabled(false);
    T24RetryJob job = new T24RetryJob(employeeOnboardingRepository, stateMachineService, disabled);

    job.run();

    verifyNoInteractions(employeeOnboardingRepository, employeeStatusHistoryRepository);
  }

  @Test
  void runSkipsWhenCapZero() {
    JobsProperties p = new JobsProperties();
    p.getT24Retry().setEnabled(true);
    p.setT24RetryAttemptsPlaceholder(0);
    p.setT24RetryBatchSize(50);
    T24RetryJob job = new T24RetryJob(employeeOnboardingRepository, stateMachineService, p);

    job.run();

    verifyNoInteractions(employeeOnboardingRepository, employeeStatusHistoryRepository);
  }

  private static EmployeeOnboarding t24Failed(String ref) {
    try {
      Constructor<EmployeeOnboarding> c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setId(900L);
      e.setEmployeeRef(ref);
      e.setStatus(OnboardingStatus.T24_FAILED);
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
