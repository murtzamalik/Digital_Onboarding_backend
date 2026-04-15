package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.AdminEmployeeSummaryResponse;
import com.bank.cebos.dto.admin.AmlComplianceReasonRequest;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.security.CorrelationIdFilter;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.admin.AdminAmlComplianceService;
import com.bank.cebos.service.admin.AdminEmployeeQueryService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
public class AdminEmployeeController {

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  private static final String AML_COMPLIANCE_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER)";

  private final AdminEmployeeQueryService adminEmployeeQueryService;
  private final AdminAmlComplianceService adminAmlComplianceService;

  public AdminEmployeeController(
      AdminEmployeeQueryService adminEmployeeQueryService,
      AdminAmlComplianceService adminAmlComplianceService) {
    this.adminEmployeeQueryService = adminEmployeeQueryService;
    this.adminAmlComplianceService = adminAmlComplianceService;
  }

  @GetMapping
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<Page<AdminEmployeeSummaryResponse>> search(
      @RequestParam(name = "status", required = false) OnboardingStatus status,
      @RequestParam(name = "q", required = false) String q,
      Pageable pageable) {
    return ResponseEntity.ok(adminEmployeeQueryService.search(status, q, pageable));
  }

  @PostMapping("/{employeeRef}/aml/clear")
  @PreAuthorize(AML_COMPLIANCE_SECURITY)
  public ResponseEntity<Void> clearAfterRejection(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("employeeRef") String employeeRef) {
    adminAmlComplianceService.clearRejectedToT24(
        employeeRef, principal.id(), MDC.get(CorrelationIdFilter.MDC_KEY));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{employeeRef}/aml/reject")
  @PreAuthorize(AML_COMPLIANCE_SECURITY)
  public ResponseEntity<Void> rejectFromPending(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("employeeRef") String employeeRef,
      @Valid @RequestBody AmlComplianceReasonRequest body) {
    adminAmlComplianceService.rejectFromAmlPending(
        employeeRef, principal.id(), body.reason(), MDC.get(CorrelationIdFilter.MDC_KEY));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{employeeRef}/aml/block")
  @PreAuthorize(AML_COMPLIANCE_SECURITY)
  public ResponseEntity<Void> blockAfterRejection(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("employeeRef") String employeeRef) {
    adminAmlComplianceService.blockAfterAmlRejection(
        employeeRef, principal.id(), MDC.get(CorrelationIdFilter.MDC_KEY));
    return ResponseEntity.noContent().build();
  }
}
