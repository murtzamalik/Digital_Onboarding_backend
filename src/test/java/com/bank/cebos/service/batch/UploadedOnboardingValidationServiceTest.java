package com.bank.cebos.service.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.Optional;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UploadedOnboardingValidationServiceTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private AmlIntegration amlIntegration;

  private UploadedOnboardingValidationService validationService;

  @BeforeEach
  void setUp() {
    StateMachineService stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(1000L);
              }
              return e;
            });
    EmployeeOnboardingService employeeOnboardingService =
        new EmployeeOnboardingService(employeeOnboardingRepository, stateMachineService);
    UploadBatchAggregateService uploadBatchAggregateService =
        new UploadBatchAggregateService(uploadBatchRepository, employeeOnboardingRepository);
    when(amlIntegration.screen(any(AmlScreeningRequest.class)))
        .thenReturn(new AmlScreeningResult(true, "MOCK-AML-OK", "LOW"));
    validationService =
        new UploadedOnboardingValidationService(
            employeeOnboardingRepository,
            employeeOnboardingService,
            uploadBatchAggregateService,
            amlIntegration);
  }

  private static EmployeeOnboarding row(
      Long id, String cnic, String mobile, String fullName) {
    try {
      var c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setId(id);
      e.setEmployeeRef("EMP-" + id);
      e.setBatchId(900L);
      e.setStatus(OnboardingStatus.UPLOADED);
      e.setCnic(cnic);
      e.setMobile(mobile);
      e.setFullName(fullName);
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void validRowTransitionsToValidated() {
    EmployeeOnboarding r = row(1L, "11111-1111111-1", "03001234567", "Ali Khan");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.UPLOADED), any(Pageable.class)))
        .thenReturn(List.of(r));
    stubAggregateAfterValidation(900L, 1, 0, 0);

    int n = validationService.processPage(50);

    org.assertj.core.api.Assertions.assertThat(n).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(r.getStatus()).isEqualTo(OnboardingStatus.VALIDATED);
    verify(amlIntegration, times(1)).screen(any(AmlScreeningRequest.class));
    verify(employeeStatusHistoryRepository).save(any());
    verify(uploadBatchRepository).save(any(UploadBatch.class));
  }

  @Test
  void amlNotClearedTransitionsToInvalid() {
    EmployeeOnboarding r = row(3L, "11111-1111111-1", "03001234567", "Ali Khan");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.UPLOADED), any(Pageable.class)))
        .thenReturn(List.of(r));
    when(amlIntegration.screen(any(AmlScreeningRequest.class)))
        .thenReturn(new AmlScreeningResult(false, "REF-HIT", "HIGH"));
    stubAggregateAfterValidation(900L, 1, 1, 0);

    int n = validationService.processPage(50);

    org.assertj.core.api.Assertions.assertThat(n).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(r.getStatus()).isEqualTo(OnboardingStatus.INVALID);
    org.assertj.core.api.Assertions.assertThat(r.getValidationErrors()).contains("AML");
    verify(uploadBatchRepository).save(any(UploadBatch.class));
  }

  @Test
  void amlThrowsTransitionsToInvalid() {
    EmployeeOnboarding r = row(4L, "11111-1111111-1", "03001234567", "Ali Khan");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.UPLOADED), any(Pageable.class)))
        .thenReturn(List.of(r));
    when(amlIntegration.screen(any(AmlScreeningRequest.class)))
        .thenThrow(new IllegalStateException("upstream"));
    stubAggregateAfterValidation(900L, 1, 1, 0);

    validationService.processPage(50);

    org.assertj.core.api.Assertions.assertThat(r.getStatus()).isEqualTo(OnboardingStatus.INVALID);
    org.assertj.core.api.Assertions.assertThat(r.getValidationErrors()).contains("unavailable");
  }

  @Test
  void invalidCnicTransitionsToInvalidWithErrors() {
    EmployeeOnboarding r = row(2L, "bad", "03001234567", "Ali Khan");
    when(employeeOnboardingRepository.findByStatus(eq(OnboardingStatus.UPLOADED), any(Pageable.class)))
        .thenReturn(List.of(r));
    stubAggregateAfterValidation(900L, 1, 1, 0);

    validationService.processPage(50);

    org.assertj.core.api.Assertions.assertThat(r.getValidationErrors()).contains("CNIC");
    org.assertj.core.api.Assertions.assertThat(r.getStatus()).isEqualTo(OnboardingStatus.INVALID);
    verify(amlIntegration, never()).screen(any());
    verify(employeeStatusHistoryRepository).save(any());
    verify(uploadBatchRepository).save(any(UploadBatch.class));
  }

  @Test
  void emptyPageReturnsZero() {
    when(employeeOnboardingRepository.findByStatus(
            eq(OnboardingStatus.UPLOADED), any(Pageable.class)))
        .thenReturn(List.of());

    org.assertj.core.api.Assertions.assertThat(validationService.processPage(50)).isZero();
    verifyNoInteractions(uploadBatchRepository);
  }

  private void stubAggregateAfterValidation(long batchId, long total, long invalid, long uploaded) {
    UploadBatch batch = new UploadBatch();
    batch.setId(batchId);
    batch.setStatus("PROCESSING");
    when(uploadBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
    when(employeeOnboardingRepository.countByBatchId(batchId)).thenReturn(total);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(batchId, OnboardingStatus.INVALID))
        .thenReturn(invalid);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(batchId, OnboardingStatus.UPLOADED))
        .thenReturn(uploaded);
  }
}
