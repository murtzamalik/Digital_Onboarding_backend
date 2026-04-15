package com.bank.cebos.job;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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

@ExtendWith(MockitoExtension.class)
class InviteExpiryJobTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  private StateMachineService stateMachineService;

  private InviteExpiryJob inviteExpiryJob;

  @BeforeEach
  void setUp() {
    stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    JobsProperties jobsProperties = new JobsProperties();
    jobsProperties.getInviteExpiry().setEnabled(true);
    inviteExpiryJob =
        new InviteExpiryJob(employeeOnboardingRepository, stateMachineService, jobsProperties);
  }

  @Test
  void runTransitionsExpiredCandidatesThroughStateMachine() {
    EmployeeOnboarding invited = onboarding("E-INV-1", OnboardingStatus.INVITED);
    EmployeeOnboarding otpPending = onboarding("E-INV-2", OnboardingStatus.OTP_PENDING);
    when(employeeOnboardingRepository.findByStatusInAndExpireAtBefore(
            eq(List.of(OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING)), any()))
        .thenReturn(List.of(invited, otpPending));

    inviteExpiryJob.run();

    verify(employeeOnboardingRepository, times(2)).save(any(EmployeeOnboarding.class));
    verify(employeeStatusHistoryRepository, times(2)).save(any());
  }

  @Test
  void runSkipsWhenJobDisabled() {
    JobsProperties disabled = new JobsProperties();
    disabled.getInviteExpiry().setEnabled(false);
    InviteExpiryJob disabledJob =
        new InviteExpiryJob(employeeOnboardingRepository, stateMachineService, disabled);

    disabledJob.run();

    verifyNoInteractions(employeeOnboardingRepository, employeeStatusHistoryRepository);
  }

  @Test
  void runDoesNothingWhenNoCandidatesFound() {
    when(employeeOnboardingRepository.findByStatusInAndExpireAtBefore(
            eq(List.of(OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING)), any()))
        .thenReturn(List.of());

    inviteExpiryJob.run();

    verify(employeeOnboardingRepository, times(1))
        .findByStatusInAndExpireAtBefore(
            eq(List.of(OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING)), any());
    verifyNoInteractions(employeeStatusHistoryRepository);
  }

  private static EmployeeOnboarding onboarding(String ref, OnboardingStatus status) {
    try {
      Constructor<EmployeeOnboarding> constructor = EmployeeOnboarding.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      EmployeeOnboarding employeeOnboarding = constructor.newInstance();
      employeeOnboarding.setEmployeeRef(ref);
      employeeOnboarding.setStatus(status);
      return employeeOnboarding;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
