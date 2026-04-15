package com.bank.cebos.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadBatchAggregateServiceTest {

  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;

  private UploadBatchAggregateService aggregateService;

  @BeforeEach
  void setUp() {
    aggregateService = new UploadBatchAggregateService(uploadBatchRepository, employeeOnboardingRepository);
  }

  @Test
  void refreshBatchSetsCountsAndReadyWhenNoUploadedLeft() {
    UploadBatch batch = new UploadBatch();
    batch.setId(1L);
    batch.setStatus("PROCESSING");
    batch.setTotalRows(4);
    when(uploadBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(employeeOnboardingRepository.countByBatchId(1L)).thenReturn(4L);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(1L, OnboardingStatus.INVALID))
        .thenReturn(1L);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(1L, OnboardingStatus.UPLOADED))
        .thenReturn(0L);

    aggregateService.refreshBatch(1L);

    ArgumentCaptor<UploadBatch> captor = ArgumentCaptor.forClass(UploadBatch.class);
    verify(uploadBatchRepository).save(captor.capture());
    UploadBatch saved = captor.getValue();
    assertThat(saved.getValidRowCount()).isEqualTo(3);
    assertThat(saved.getInvalidRowCount()).isEqualTo(1);
    assertThat(saved.getStatus()).isEqualTo(UploadBatchAggregateService.BATCH_STATUS_READY);
  }

  @Test
  void refreshBatchSkippedWhenBatchMissing() {
    when(uploadBatchRepository.findById(99L)).thenReturn(Optional.empty());
    aggregateService.refreshBatch(99L);
    verify(uploadBatchRepository).findById(99L);
    org.mockito.Mockito.verifyNoMoreInteractions(uploadBatchRepository);
  }
}
