package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.AdminEmployeeSummaryResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEmployeeQueryService {

  private final EmployeeOnboardingRepository employeeOnboardingRepository;

  public AdminEmployeeQueryService(EmployeeOnboardingRepository employeeOnboardingRepository) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
  }

  @Transactional(readOnly = true)
  public Page<AdminEmployeeSummaryResponse> search(
      OnboardingStatus status, String q, Pageable pageable) {
    String query = q != null && !q.isBlank() ? q.trim() : null;
    return employeeOnboardingRepository
        .adminSearch(status, query, pageable)
        .map(AdminEmployeeQueryService::toSummary);
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
}
