package com.bank.cebos.service.batch;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves {@link OnboardingStatus#UPLOADED} rows to {@link OnboardingStatus#VALIDATED} or {@link
 * OnboardingStatus#INVALID} using minimal row rules. All status changes go through {@link
 * com.bank.cebos.statemachine.StateMachineService} via {@link EmployeeOnboardingService}.
 */
@Service
public class UploadedOnboardingValidationService {

  private static final Logger log = LoggerFactory.getLogger(UploadedOnboardingValidationService.class);

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final EmployeeOnboardingService employeeOnboardingService;
  private final UploadBatchAggregateService uploadBatchAggregateService;
  private final AmlIntegration amlIntegration;

  public UploadedOnboardingValidationService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      EmployeeOnboardingService employeeOnboardingService,
      UploadBatchAggregateService uploadBatchAggregateService,
      AmlIntegration amlIntegration) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.employeeOnboardingService = employeeOnboardingService;
    this.uploadBatchAggregateService = uploadBatchAggregateService;
    this.amlIntegration = amlIntegration;
  }

  /**
   * @return number of rows processed (validated or invalidated)
   */
  @Transactional
  public int processPage(int pageSize) {
    if (pageSize <= 0) {
      return 0;
    }
    List<EmployeeOnboarding> page =
        employeeOnboardingRepository.findByStatus(
            OnboardingStatus.UPLOADED, PageRequest.of(0, pageSize));
    if (page.isEmpty()) {
      return 0;
    }
    int processed = 0;
    Set<Long> batchIds = new HashSet<>();
    for (EmployeeOnboarding row : page) {
      List<String> errors =
          BulkUploadRowRules.validate(row.getCnic(), row.getMobile(), row.getFullName());
      if (errors.isEmpty()) {
        if (!runAmlAndDecide(row)) {
          batchIds.add(row.getBatchId());
          processed++;
          continue;
        }
        row.setValidationErrors(null);
        employeeOnboardingService.transition(
            row,
            OnboardingStatus.VALIDATED,
            "scheduler:uploaded-validation",
            "Bulk upload row passed validation");
      } else {
        row.setValidationErrors(String.join("; ", errors));
        employeeOnboardingService.transition(
            row,
            OnboardingStatus.INVALID,
            "scheduler:uploaded-validation",
            "Bulk upload row failed validation");
      }
      batchIds.add(row.getBatchId());
      processed++;
    }
    for (Long batchId : batchIds) {
      uploadBatchAggregateService.refreshBatch(batchId);
    }
    return processed;
  }

  /**
   * @return true if the row should proceed to VALIDATED; false if transitioned to INVALID
   */
  private boolean runAmlAndDecide(EmployeeOnboarding row) {
    AmlScreeningResult aml;
    try {
      aml =
          amlIntegration.screen(
              new AmlScreeningRequest(
                  row.getFullName(),
                  row.getCnic(),
                  Optional.ofNullable(row.getPresentCountry()).orElse("")));
    } catch (Exception ex) {
      log.warn("AML screening failed for employeeRef={}", row.getEmployeeRef(), ex);
      row.setValidationErrors("AML screening is temporarily unavailable.");
      employeeOnboardingService.transition(
          row,
          OnboardingStatus.INVALID,
          "scheduler:uploaded-validation",
          "Bulk upload row failed AML screening (integration error)");
      return false;
    }
    if (!aml.cleared()) {
      row.setValidationErrors(
          "AML screening did not clear (reference "
              + aml.screeningReference()
              + ", risk "
              + aml.riskBand()
              + ").");
      employeeOnboardingService.transition(
          row,
          OnboardingStatus.INVALID,
          "scheduler:uploaded-validation",
          "Bulk upload row failed AML screening");
      return false;
    }
    return true;
  }
}
