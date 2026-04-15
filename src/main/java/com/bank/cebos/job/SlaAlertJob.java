package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaAlertJob {

  private static final Logger log = LoggerFactory.getLogger(SlaAlertJob.class);

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final JobsProperties jobsProperties;

  public SlaAlertJob(
      EmployeeOnboardingRepository employeeOnboardingRepository, JobsProperties jobsProperties) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.jobsProperties = jobsProperties;
  }

  @Scheduled(cron = "${cebos.jobs.sla-alert.cron}")
  public void run() {
    if (!jobsProperties.getSlaAlert().isEnabled()) {
      return;
    }

    Instant cutoff = Instant.now().minus(jobsProperties.getSlaAlertDays(), ChronoUnit.DAYS);
    List<EmployeeOnboarding> staleInvites =
        employeeOnboardingRepository.findInvitedOlderThan(OnboardingStatus.INVITED, cutoff);

    log.warn(
        "SlaAlertJob placeholder; staleInvites={} thresholdDays={}",
        staleInvites.size(),
        jobsProperties.getSlaAlertDays());
  }
}
