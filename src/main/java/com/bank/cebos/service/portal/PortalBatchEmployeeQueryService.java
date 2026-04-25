package com.bank.cebos.service.portal;

import com.bank.cebos.dto.portal.PortalBatchEmployeeRowResponse;
import com.bank.cebos.dto.portal.PortalEmployeeDetailResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.util.PiiMasking;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalBatchEmployeeQueryService {

  private final UploadBatchRepository uploadBatchRepository;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;

  public PortalBatchEmployeeQueryService(
      UploadBatchRepository uploadBatchRepository,
      EmployeeOnboardingRepository employeeOnboardingRepository) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
  }

  @Transactional(readOnly = true)
  public Optional<Page<PortalBatchEmployeeRowResponse>> listEmployeesForBatch(
      String batchReference, long corporateClientId, Pageable pageable) {
    Optional<UploadBatch> batchOpt =
        uploadBatchRepository.findByBatchReferenceAndCorporateClientId(
            batchReference, corporateClientId);
    if (batchOpt.isEmpty()) {
      return Optional.empty();
    }
    UploadBatch batch = batchOpt.get();
    Page<PortalBatchEmployeeRowResponse> page =
        employeeOnboardingRepository
            .findByBatchId(batch.getId(), pageable)
            .map(PortalBatchEmployeeQueryService::toRow);
    return Optional.of(page);
  }

  @Transactional(readOnly = true)
  public Optional<PortalEmployeeDetailResponse> getEmployeeDetail(
      String batchReference, String employeeRef, long corporateClientId) {
    Optional<UploadBatch> batchOpt =
        uploadBatchRepository.findByBatchReferenceAndCorporateClientId(
            batchReference, corporateClientId);
    if (batchOpt.isEmpty()) {
      return Optional.empty();
    }
    UploadBatch batch = batchOpt.get();
    return employeeOnboardingRepository
        .findByEmployeeRef(employeeRef)
        .filter(
            e ->
                batch.getId().equals(e.getBatchId())
                    && Objects.equals(corporateClientId, e.getCorporateClientId()))
        .map(e -> toPortalDetail(e, batch.getBatchReference()));
  }

  private static PortalBatchEmployeeRowResponse toRow(EmployeeOnboarding e) {
    return new PortalBatchEmployeeRowResponse(
        e.getEmployeeRef(),
        e.getStatus() != null ? e.getStatus().name() : "",
        e.getFullName(),
        PiiMasking.maskTail(e.getMobile(), 4),
        PiiMasking.maskTail(e.getCnic(), 4),
        e.getEmail(),
        e.getInviteSentAt(),
        e.getExpireAt(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private static PortalEmployeeDetailResponse toPortalDetail(
      EmployeeOnboarding e, String batchReference) {
    return new PortalEmployeeDetailResponse(
        e.getEmployeeRef(),
        batchReference,
        e.getStatus() != null ? e.getStatus().name() : "",
        e.getFullName(),
        e.getFatherName(),
        e.getMotherName(),
        e.getDateOfBirth(),
        e.getGender(),
        e.getReligion(),
        PiiMasking.maskTail(e.getMobile(), 4),
        PiiMasking.maskTail(e.getCnic(), 4),
        e.getEmail(),
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
        e.getAmlScreeningStatus(),
        e.getAmlCaseReference(),
        e.getT24CustomerId(),
        e.getT24AccountId(),
        e.getT24SubmissionStatus(),
        e.getValidationErrors(),
        e.getFormDataJson(),
        e.getInviteSentAt(),
        e.getExpireAt(),
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
