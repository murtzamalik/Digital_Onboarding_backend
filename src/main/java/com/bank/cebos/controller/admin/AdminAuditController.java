package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.AdminStatusHistoryResponse;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  private final EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  public AdminAuditController(EmployeeStatusHistoryRepository employeeStatusHistoryRepository) {
    this.employeeStatusHistoryRepository = employeeStatusHistoryRepository;
  }

  @GetMapping("/employee-status-history")
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<Page<AdminStatusHistoryResponse>> recentEmployeeStatus(Pageable pageable) {
    return ResponseEntity.ok(employeeStatusHistoryRepository.findRecentForAudit(pageable));
  }
}
