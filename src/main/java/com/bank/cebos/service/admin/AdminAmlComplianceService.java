package com.bank.cebos.service.admin;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.statemachine.StateMachineService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAmlComplianceService {

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final StateMachineService stateMachineService;

  public AdminAmlComplianceService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      StateMachineService stateMachineService) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.stateMachineService = stateMachineService;
  }

  /**
   * Compliance clears an AML rejection: two transitions so {@code employee_status_history} records
   * both the reopen and the advance to core banking (double audit trail without a separate table).
   */
  @Transactional
  public void clearRejectedToT24(String employeeRef, long bankAdminUserId, String correlationId) {
    EmployeeOnboarding employee = loadByRef(employeeRef);
    if (employee.getStatus() != OnboardingStatus.AML_REJECTED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Employee must be in AML_REJECTED status to clear");
    }
    String changedBy = "bank-admin:" + bankAdminUserId;
    stateMachineService.transition(
        employee,
        OnboardingStatus.AML_CHECK_PENDING,
        changedBy,
        "AML compliance: reopened after manual review",
        true,
        null,
        correlationId);
    stateMachineService.transition(
        employee,
        OnboardingStatus.T24_PENDING,
        changedBy,
        "AML compliance: cleared to core banking (T24)",
        false,
        null,
        correlationId);
  }

  @Transactional
  public void rejectFromAmlPending(String employeeRef, long bankAdminUserId, String reason, String correlationId) {
    EmployeeOnboarding employee = loadByRef(employeeRef);
    if (employee.getStatus() != OnboardingStatus.AML_CHECK_PENDING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Employee must be in AML_CHECK_PENDING status to reject");
    }
    String changedBy = "bank-admin:" + bankAdminUserId;
    stateMachineService.transition(
        employee,
        OnboardingStatus.AML_REJECTED,
        changedBy,
        reason,
        false,
        null,
        correlationId);
  }

  @Transactional
  public void blockAfterAmlRejection(String employeeRef, long bankAdminUserId, String correlationId) {
    EmployeeOnboarding employee = loadByRef(employeeRef);
    if (employee.getStatus() != OnboardingStatus.AML_REJECTED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Employee must be in AML_REJECTED status to block");
    }
    String changedBy = "bank-admin:" + bankAdminUserId;
    stateMachineService.transition(
        employee,
        OnboardingStatus.BLOCKED,
        changedBy,
        "AML compliance: confirmed block after rejection",
        false,
        null,
        correlationId);
  }

  private EmployeeOnboarding loadByRef(String employeeRef) {
    return employeeOnboardingRepository
        .findByEmployeeRef(employeeRef)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
  }
}
