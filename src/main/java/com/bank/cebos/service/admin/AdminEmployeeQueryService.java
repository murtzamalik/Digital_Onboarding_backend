package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.AdminEmployeeDetailResponse;
import com.bank.cebos.dto.admin.AdminEmployeeSummaryResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEmployeeQueryService {

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final UploadBatchRepository uploadBatchRepository;

  public AdminEmployeeQueryService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      UploadBatchRepository uploadBatchRepository) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.uploadBatchRepository = uploadBatchRepository;
  }

  @Transactional(readOnly = true)
  public Page<AdminEmployeeSummaryResponse> search(
      OnboardingStatus status, String q, Pageable pageable) {
    String query = q != null && !q.isBlank() ? q.trim() : null;
    return employeeOnboardingRepository
        .adminSearch(status, query, pageable)
        .map(AdminEmployeeQueryService::toSummary);
  }

  @Transactional(readOnly = true)
  public Optional<AdminEmployeeDetailResponse> findDetailByEmployeeRef(String employeeRef) {
    return employeeOnboardingRepository.findByEmployeeRef(employeeRef).map(this::toDetail);
  }

  private static AdminEmployeeSummaryResponse toSummary(EmployeeOnboarding e) {
    return new AdminEmployeeSummaryResponse(
        e.getId(),
        e.getEmployeeRef(),
        e.getStatus(),
        e.getCorporateClientId(),
        e.getFullName(),
        e.getAmlScreeningStatus(),
        e.getAmlCaseReference());
  }

  private AdminEmployeeDetailResponse toDetail(EmployeeOnboarding e) {
    String batchReference =
        uploadBatchRepository
            .findById(e.getBatchId())
            .map(UploadBatch::getBatchReference)
            .orElse("");
    return new AdminEmployeeDetailResponse(
        e.getEmployeeRef(),
        e.getStatus() != null ? e.getStatus().name() : "",
        e.getCorporateClientId() != null ? e.getCorporateClientId() : 0L,
        batchReference,
        e.getCorrectionBatchId(),
        e.getFullName(),
        e.getFatherName(),
        e.getMotherName(),
        e.getMobile(),
        e.getEmail(),
        e.getCnic(),
        e.getDateOfBirth(),
        e.getGender(),
        e.getReligion(),
        e.getCnicIssueDate(),
        e.getCnicExpiryDate(),
        e.getPresentAddressLine1(),
        e.getPresentAddressLine2(),
        e.getPresentCity(),
        e.getPresentCountry(),
        e.getPermanentAddressLine1(),
        e.getPermanentAddressLine2(),
        e.getPermanentCity(),
        e.getPermanentCountry(),
        e.getNadraTransactionId(),
        e.getNadraVerificationStatus(),
        e.getNadraVerifiedAt(),
        e.getLivenessResult(),
        e.getLivenessScore(),
        e.getLivenessCompletedAt(),
        e.getFaceMatchResult(),
        e.getFaceMatchScore(),
        e.getFaceMatchCompletedAt(),
        e.getFingerprintTemplateRef(),
        e.getFingerprintMatchResult(),
        e.getFingerprintQualityScore(),
        e.getFingerprintCompletedAt(),
        e.getQuizScore(),
        e.getQuizMaxScore(),
        e.getQuizPassed(),
        e.getQuizCompletedAt(),
        e.getAmlScreeningStatus(),
        e.getAmlCaseReference(),
        e.getAmlLastCheckedAt(),
        e.getAmlScreeningSummary(),
        e.getT24CustomerId(),
        e.getT24AccountId(),
        e.getT24SubmissionStatus(),
        e.getT24LastError(),
        e.getT24LastAttemptAt(),
        e.getOcrStatus(),
        e.getValidationErrors(),
        e.getFormDataJson(),
        e.getBlockReason(),
        e.getBlockedAt(),
        e.getUnblockedBy(),
        e.getUnblockedAt(),
        e.getExpireAt(),
        e.getInviteSentAt(),
        e.getInviteResendCount() != null ? e.getInviteResendCount() : 0,
        e.getCreatedAt(),
        e.getUpdatedAt(),
        hasImage(e.getCnicFrontImageData(), e.getCnicFrontImagePath()),
        hasImage(e.getCnicBackImageData(), e.getCnicBackImagePath()),
        hasImage(e.getSelfieImageData(), e.getSelfieImagePath()));
  }

  private static boolean hasImage(byte[] data, String path) {
    if (data != null && data.length > 0) {
      return true;
    }
    return path != null && !path.isBlank();
  }
}
