package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.statemachine.StateMachineService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InviteExpiryJob {

  private static final Logger log = LoggerFactory.getLogger(InviteExpiryJob.class);
  private static final String CHANGED_BY = "scheduler:invite-expiry";

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final StateMachineService stateMachineService;
  private final JobsProperties jobsProperties;

  public InviteExpiryJob(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      StateMachineService stateMachineService,
      JobsProperties jobsProperties) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.stateMachineService = stateMachineService;
    this.jobsProperties = jobsProperties;
  }

  @Scheduled(cron = "${cebos.jobs.invite-expiry.cron}")
  @Transactional
  public void run() {
    if (!jobsProperties.getInviteExpiry().isEnabled()) {
      return;
    }

    List<EmployeeOnboarding> expiredCandidates =
        employeeOnboardingRepository.findByStatusInAndExpireAtBefore(
            List.of(OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING), Instant.now());

    int transitioned = 0;
    for (EmployeeOnboarding employee : expiredCandidates) {
      stateMachineService.transition(
          employee,
          OnboardingStatus.EXPIRED,
          CHANGED_BY,
          "Invite/OTP window expired by scheduler");
      transitioned++;
    }
    log.info(
        "InviteExpiryJob completed; candidates={} transitioned={}",
        expiredCandidates.size(),
        transitioned);
  }
}
