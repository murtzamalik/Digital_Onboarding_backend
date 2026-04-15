package com.bank.cebos.job;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.bank.cebos.statemachine.StateMachineService;
import com.bank.cebos.util.PhoneE164;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional bulk step: {@link OnboardingStatus#VALIDATED} → {@link OnboardingStatus#INVITED}. Disabled by
 * default. Enqueues {@link OutboxEventType#SMS_SEND} when a normalizable mobile is present (mock SMS in
 * dev; wire real SMS under profile {@code real}).
 */
@Component
public class InviteDispatchJob {

  private static final Logger log = LoggerFactory.getLogger(InviteDispatchJob.class);
  private static final String CHANGED_BY = "scheduler:invite-dispatch";

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final StateMachineService stateMachineService;
  private final JobsProperties jobsProperties;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;

  public InviteDispatchJob(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      StateMachineService stateMachineService,
      JobsProperties jobsProperties,
      OutboxService outboxService,
      ObjectMapper objectMapper) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.stateMachineService = stateMachineService;
    this.jobsProperties = jobsProperties;
    this.outboxService = outboxService;
    this.objectMapper = objectMapper;
  }

  @Scheduled(cron = "${cebos.jobs.invite-dispatch.cron}")
  @Transactional
  public void run() {
    if (!jobsProperties.getInviteDispatch().isEnabled()) {
      return;
    }
    int batchSize = Math.max(1, jobsProperties.getInviteDispatchBatchSize());
    List<EmployeeOnboarding> page =
        employeeOnboardingRepository.findByStatus(
            OnboardingStatus.VALIDATED, PageRequest.of(0, batchSize));
    if (page.isEmpty()) {
      return;
    }
    int n = 0;
    for (EmployeeOnboarding employee : page) {
      stateMachineService.transition(
          employee,
          OnboardingStatus.INVITED,
          CHANGED_BY,
          "Bulk invite dispatch (validated employees)");
      enqueueInviteSms(employee);
      n++;
    }
    log.info("InviteDispatchJob transitioned {} VALIDATED row(s) to INVITED", n);
  }

  private void enqueueInviteSms(EmployeeOnboarding employee) {
    String to = PhoneE164.toE164(employee.getMobile());
    if (to == null) {
      return;
    }
    try {
      Map<String, String> payload = new LinkedHashMap<>();
      payload.put("toE164", to);
      payload.put(
          "body",
          "CEBOS onboarding: continue with employee ref "
              + employee.getEmployeeRef()
              + " (invite sent).");
      outboxService.enqueue(
          OutboxEventType.SMS_SEND,
          "EmployeeOnboarding",
          String.valueOf(employee.getId()),
          objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize SMS outbox payload for employee {}", employee.getId(), e);
    }
  }

}
