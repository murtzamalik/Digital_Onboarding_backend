package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.statemachine.StateMachineService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class T24RetryJob {

  private static final Logger log = LoggerFactory.getLogger(T24RetryJob.class);
  private static final String CHANGED_BY = "scheduler:t24-retry";

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final StateMachineService stateMachineService;
  private final JobsProperties jobsProperties;

  public T24RetryJob(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      StateMachineService stateMachineService,
      JobsProperties jobsProperties) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.stateMachineService = stateMachineService;
    this.jobsProperties = jobsProperties;
  }

  @Scheduled(cron = "${cebos.jobs.t24-retry.cron}")
  @Transactional
  public void run() {
    if (!jobsProperties.getT24Retry().isEnabled()) {
      return;
    }

    int retryBudget = jobsProperties.getT24RetryAttemptsPlaceholder();
    int batchSize = jobsProperties.getT24RetryBatchSize();
    int cap = Math.max(0, Math.min(retryBudget, batchSize));
    if (cap == 0) {
      log.info("T24RetryJob skipped; cap=0");
      return;
    }

    List<EmployeeOnboarding> failed =
        employeeOnboardingRepository.findByStatus(
            OnboardingStatus.T24_FAILED, PageRequest.of(0, cap));
    int transitioned = 0;
    for (EmployeeOnboarding employee : failed) {
      stateMachineService.transition(
          employee,
          OnboardingStatus.T24_PENDING,
          CHANGED_BY,
          "Scheduled T24 retry (attempt placeholder)");
      transitioned++;
    }
    log.info(
        "T24RetryJob completed; candidates={} transitioned={} cap={}",
        failed.size(),
        transitioned,
        cap);
  }
}
