package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.AdminDashboardSummaryResponse;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.repository.CorrectionBatchRepository;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

  private static final List<OnboardingStatus> NOT_IN_PROGRESS =
      List.of(OnboardingStatus.ACCOUNT_OPENED, OnboardingStatus.EXPIRED);

  private final CorporateClientRepository corporateClientRepository;
  private final UploadBatchRepository uploadBatchRepository;
  private final CorrectionBatchRepository correctionBatchRepository;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;

  public AdminDashboardService(
      CorporateClientRepository corporateClientRepository,
      UploadBatchRepository uploadBatchRepository,
      CorrectionBatchRepository correctionBatchRepository,
      EmployeeOnboardingRepository employeeOnboardingRepository) {
    this.corporateClientRepository = corporateClientRepository;
    this.uploadBatchRepository = uploadBatchRepository;
    this.correctionBatchRepository = correctionBatchRepository;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
  }

  public AdminDashboardSummaryResponse summary() {
    return new AdminDashboardSummaryResponse(
        corporateClientRepository.count(),
        uploadBatchRepository.count(),
        correctionBatchRepository.count(),
        employeeOnboardingRepository.countByStatusNotIn(NOT_IN_PROGRESS));
  }
}
