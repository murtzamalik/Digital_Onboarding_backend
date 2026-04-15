package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.service.batch.UploadedOnboardingValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OnboardingUploadedValidationJob {

  private static final Logger log = LoggerFactory.getLogger(OnboardingUploadedValidationJob.class);

  private final UploadedOnboardingValidationService uploadedOnboardingValidationService;
  private final JobsProperties jobsProperties;

  public OnboardingUploadedValidationJob(
      UploadedOnboardingValidationService uploadedOnboardingValidationService,
      JobsProperties jobsProperties) {
    this.uploadedOnboardingValidationService = uploadedOnboardingValidationService;
    this.jobsProperties = jobsProperties;
  }

  @Scheduled(cron = "${cebos.jobs.uploaded-validation.cron}")
  public void run() {
    if (!jobsProperties.getUploadedValidation().isEnabled()) {
      return;
    }
    int processed =
        uploadedOnboardingValidationService.processPage(jobsProperties.getUploadedValidationBatchSize());
    if (processed > 0) {
      log.info("OnboardingUploadedValidationJob processed {} UPLOADED row(s)", processed);
    }
  }
}
