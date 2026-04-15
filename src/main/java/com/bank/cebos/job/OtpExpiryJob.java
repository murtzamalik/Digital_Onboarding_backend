package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.repository.OtpSessionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OtpExpiryJob {

  private static final Logger log = LoggerFactory.getLogger(OtpExpiryJob.class);

  private final OtpSessionRepository otpSessionRepository;
  private final JobsProperties jobsProperties;

  public OtpExpiryJob(OtpSessionRepository otpSessionRepository, JobsProperties jobsProperties) {
    this.otpSessionRepository = otpSessionRepository;
    this.jobsProperties = jobsProperties;
  }

  @Scheduled(cron = "${cebos.jobs.otp-expiry.cron}")
  @Transactional
  public void run() {
    if (!jobsProperties.getOtpExpiry().isEnabled()) {
      return;
    }
    int expired = otpSessionRepository.expireActiveSessions(Instant.now());
    log.info("OtpExpiryJob completed; expiredSessions={}", expired);
  }
}
