package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.service.invite.InviteDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional bulk step: {@link com.bank.cebos.enums.OnboardingStatus#VALIDATED} → {@link
 * com.bank.cebos.enums.OnboardingStatus#INVITED}. Disabled by default. Delegates to {@link InviteDispatchService}.
 */
@Component
public class InviteDispatchJob {

  private static final Logger log = LoggerFactory.getLogger(InviteDispatchJob.class);
  private static final String CHANGED_BY = "scheduler:invite-dispatch";

  private final JobsProperties jobsProperties;
  private final InviteDispatchService inviteDispatchService;

  public InviteDispatchJob(JobsProperties jobsProperties, InviteDispatchService inviteDispatchService) {
    this.jobsProperties = jobsProperties;
    this.inviteDispatchService = inviteDispatchService;
  }

  @Scheduled(cron = "${cebos.jobs.invite-dispatch.cron}")
  public void run() {
    if (!jobsProperties.getInviteDispatch().isEnabled()) {
      return;
    }
    int n =
        inviteDispatchService.dispatchValidatedGlobally(
            CHANGED_BY, "Bulk invite dispatch (validated employees)");
    log.info("InviteDispatchJob transitioned {} VALIDATED row(s) to INVITED", n);
  }
}
