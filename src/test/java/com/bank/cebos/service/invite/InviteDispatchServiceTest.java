package com.bank.cebos.service.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteDispatchServiceTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private StateMachineService stateMachineService;
  @Mock private OutboxService outboxService;

  private InviteDispatchService inviteDispatchService;

  @BeforeEach
  void setUp() {
    JobsProperties jobsProperties = new JobsProperties();
    jobsProperties.setInviteDispatchBatchSize(10);
    inviteDispatchService =
        new InviteDispatchService(
            employeeOnboardingRepository,
            uploadBatchRepository,
            stateMachineService,
            jobsProperties,
            outboxService,
            new ObjectMapper());
  }

  @Test
  void dispatchForOwnedBatchTransitionsValidatedEmployees() {
    UploadBatch batch = new UploadBatch();
    batch.setId(9L);
    batch.setBatchReference("B-1");
    when(uploadBatchRepository.findByBatchReferenceAndCorporateClientId("B-1", 100L))
        .thenReturn(Optional.of(batch));
    EmployeeOnboarding e = new EmployeeOnboarding();
    e.setId(1L);
    e.setEmployeeRef("E-1");
    e.setMobile("03001234567");
    e.setStatus(OnboardingStatus.VALIDATED);
    when(employeeOnboardingRepository.findByBatchIdAndStatus(9L, OnboardingStatus.VALIDATED))
        .thenReturn(List.of(e));

    PortalBatchInviteDispatchResponse res =
        inviteDispatchService.dispatchValidatedForOwnedBatch("B-1", 100L, "portalUser:5");

    assertThat(res.attempted()).isEqualTo(1);
    assertThat(res.transitioned()).isEqualTo(1);
    assertThat(res.smsEnqueued()).isEqualTo(1);
    assertThat(res.transitionErrors()).isZero();
    verify(stateMachineService)
        .transition(eq(e), eq(OnboardingStatus.INVITED), eq("portalUser:5"), any());
    verify(outboxService)
        .enqueue(eq(OutboxEventType.SMS_SEND), eq("EmployeeOnboarding"), eq("1"), any());
  }

  @Test
  void dispatchForUnknownBatchThrows() {
    when(uploadBatchRepository.findByBatchReferenceAndCorporateClientId("X", 1L))
        .thenReturn(Optional.empty());
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> inviteDispatchService.dispatchValidatedForOwnedBatch("X", 1L, "portalUser:1"));
  }
}
