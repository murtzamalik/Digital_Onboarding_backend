package com.bank.cebos.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import com.bank.cebos.util.PhoneE164;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InviteDispatchJobTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
  @Mock private OutboxService outboxService;

  private StateMachineService stateMachineService;
  private InviteDispatchJob inviteDispatchJob;

  @BeforeEach
  void setUp() {
    stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    JobsProperties jobsProperties = new JobsProperties();
    jobsProperties.getInviteDispatch().setEnabled(true);
    jobsProperties.setInviteDispatchBatchSize(20);
    inviteDispatchJob =
        new InviteDispatchJob(
            employeeOnboardingRepository,
            stateMachineService,
            jobsProperties,
            outboxService,
            new ObjectMapper());
  }

  @Test
  void runTransitionsValidatedToInvited() {
    EmployeeOnboarding row = validated("E-V-1");
    row.setMobile("03001234567");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.VALIDATED), any(Pageable.class)))
        .thenReturn(List.of(row));

    inviteDispatchJob.run();

    verify(employeeOnboardingRepository).save(row);
    verify(employeeStatusHistoryRepository).save(any());
    verify(outboxService)
        .enqueue(
            eq(OutboxEventType.SMS_SEND),
            eq("EmployeeOnboarding"),
            eq("77"),
            anyString());
    org.assertj.core.api.Assertions.assertThat(row.getStatus()).isEqualTo(OnboardingStatus.INVITED);
  }

  @Test
  void runSkipsWhenDisabled() {
    JobsProperties disabled = new JobsProperties();
    disabled.getInviteDispatch().setEnabled(false);
    InviteDispatchJob job =
        new InviteDispatchJob(
            employeeOnboardingRepository,
            stateMachineService,
            disabled,
            outboxService,
            new ObjectMapper());

    job.run();

    verifyNoInteractions(employeeOnboardingRepository, outboxService);
  }

  @Test
  void toE164NormalizesLocalPakistanMobile() {
    org.assertj.core.api.Assertions.assertThat(PhoneE164.toE164("03001234567"))
        .isEqualTo("+923001234567");
  }

  private static EmployeeOnboarding validated(String ref) {
    try {
      Constructor<EmployeeOnboarding> c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setId(77L);
      e.setEmployeeRef(ref);
      e.setStatus(OnboardingStatus.VALIDATED);
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
