package com.bank.cebos.service.invite;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.dto.portal.PortalBatchInviteDispatchResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteDispatchService {

  private static final Logger log = LoggerFactory.getLogger(InviteDispatchService.class);

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final UploadBatchRepository uploadBatchRepository;
  private final StateMachineService stateMachineService;
  private final JobsProperties jobsProperties;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;

  public InviteDispatchService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      UploadBatchRepository uploadBatchRepository,
      StateMachineService stateMachineService,
      JobsProperties jobsProperties,
      OutboxService outboxService,
      ObjectMapper objectMapper) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.uploadBatchRepository = uploadBatchRepository;
    this.stateMachineService = stateMachineService;
    this.jobsProperties = jobsProperties;
    this.outboxService = outboxService;
    this.objectMapper = objectMapper;
  }

  /**
   * Scheduler path: transition up to {@link JobsProperties#getInviteDispatchBatchSize()} global VALIDATED
   * rows (oldest page).
   */
  @Transactional
  public int dispatchValidatedGlobally(String changedBy, String reason) {
    int batchSize = Math.max(1, jobsProperties.getInviteDispatchBatchSize());
    List<EmployeeOnboarding> page =
        employeeOnboardingRepository.findByStatus(
            OnboardingStatus.VALIDATED, PageRequest.of(0, batchSize));
    int n = 0;
    for (EmployeeOnboarding employee : page) {
      if (transitionToInvited(employee, changedBy, reason)) {
        n++;
      }
    }
    return n;
  }

  /**
   * Portal ADMIN: transition VALIDATED employees for a single upload batch owned by the corporate client.
   */
  @Transactional
  public PortalBatchInviteDispatchResponse dispatchValidatedForOwnedBatch(
      String batchReference, long corporateClientId, String changedBy) {
    UploadBatch batch =
        uploadBatchRepository
            .findByBatchReferenceAndCorporateClientId(batchReference, corporateClientId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown batch " + batchReference + " for corporate client " + corporateClientId));
    int max = Math.max(1, jobsProperties.getInviteDispatchBatchSize());
    List<EmployeeOnboarding> validated =
        employeeOnboardingRepository.findByBatchIdAndStatus(batch.getId(), OnboardingStatus.VALIDATED);
    int attempted = 0;
    int transitioned = 0;
    int smsEnqueued = 0;
    int transitionErrors = 0;
    String reason = "Portal bulk invite for batch " + batchReference;
    for (EmployeeOnboarding employee : validated) {
      if (attempted >= max) {
        break;
      }
      attempted++;
      try {
        stateMachineService.transition(employee, OnboardingStatus.INVITED, changedBy, reason);
        if (enqueueInviteSms(employee)) {
          smsEnqueued++;
        }
        transitioned++;
      } catch (RuntimeException ex) {
        transitionErrors++;
        log.warn(
            "Invite dispatch skipped for employee {} in batch {}: {}",
            employee.getId(),
            batchReference,
            ex.getMessage());
      }
    }
    return new PortalBatchInviteDispatchResponse(attempted, transitioned, smsEnqueued, transitionErrors);
  }

  private boolean transitionToInvited(EmployeeOnboarding employee, String changedBy, String reason) {
    try {
      stateMachineService.transition(employee, OnboardingStatus.INVITED, changedBy, reason);
      enqueueInviteSms(employee);
      return true;
    } catch (RuntimeException ex) {
      log.warn(
          "Global invite dispatch skipped for employee {}: {}", employee.getId(), ex.getMessage());
      return false;
    }
  }

  /** @return true when an SMS payload was written to the outbox */
  private boolean enqueueInviteSms(EmployeeOnboarding employee) {
    String to = PhoneE164.toE164(employee.getMobile());
    if (to == null) {
      return false;
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
      return true;
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize SMS outbox payload for employee {}", employee.getId(), e);
      return false;
    }
  }
}
