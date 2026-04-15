package com.bank.cebos.service.batch;

import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recomputes {@link UploadBatch} counters from {@link com.bank.cebos.entity.EmployeeOnboarding} rows
 * and advances batch status when no rows remain {@link OnboardingStatus#UPLOADED}.
 */
@Service
public class UploadBatchAggregateService {

  /** All employees parsed from the file have left the upload-validation queue. */
  public static final String BATCH_STATUS_READY = "READY";

  private final UploadBatchRepository uploadBatchRepository;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;

  public UploadBatchAggregateService(
      UploadBatchRepository uploadBatchRepository,
      EmployeeOnboardingRepository employeeOnboardingRepository) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
  }

  @Transactional
  public void refreshBatch(Long batchId) {
    if (batchId == null) {
      return;
    }
    UploadBatch batch = uploadBatchRepository.findById(batchId).orElse(null);
    if (batch == null) {
      return;
    }
    long total = employeeOnboardingRepository.countByBatchId(batchId);
    if (total <= 0) {
      return;
    }
    long invalid = employeeOnboardingRepository.countByBatchIdAndStatus(batchId, OnboardingStatus.INVALID);
    long uploaded = employeeOnboardingRepository.countByBatchIdAndStatus(batchId, OnboardingStatus.UPLOADED);
    long validLike = total - invalid - uploaded;
    batch.setValidRowCount((int) Math.min(Integer.MAX_VALUE, Math.max(0, validLike)));
    batch.setInvalidRowCount((int) Math.min(Integer.MAX_VALUE, invalid));
    if (uploaded == 0) {
      batch.setStatus(BATCH_STATUS_READY);
    }
    uploadBatchRepository.save(batch);
  }
}
