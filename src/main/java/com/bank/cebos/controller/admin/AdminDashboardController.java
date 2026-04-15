package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.AdminDashboardSummaryResponse;
import com.bank.cebos.service.admin.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

  private final AdminDashboardService adminDashboardService;

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  public AdminDashboardController(AdminDashboardService adminDashboardService) {
    this.adminDashboardService = adminDashboardService;
  }

  @GetMapping("/summary")
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<AdminDashboardSummaryResponse> summary() {
    return ResponseEntity.ok(adminDashboardService.summary());
  }
}
