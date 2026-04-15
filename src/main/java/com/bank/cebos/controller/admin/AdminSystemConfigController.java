package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.AdminSystemConfigEntryResponse;
import com.bank.cebos.dto.admin.UpdateSystemConfigRequest;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.admin.AdminSystemConfigService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/config")
public class AdminSystemConfigController {

  private static final String CONFIG_READ_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  private static final String CONFIG_WRITE_SECURITY =
      "@principalAccess.hasBankAdminRole(T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN)";

  private final AdminSystemConfigService adminSystemConfigService;

  public AdminSystemConfigController(AdminSystemConfigService adminSystemConfigService) {
    this.adminSystemConfigService = adminSystemConfigService;
  }

  @GetMapping
  @PreAuthorize(CONFIG_READ_SECURITY)
  public ResponseEntity<Page<AdminSystemConfigEntryResponse>> list(Pageable pageable) {
    return ResponseEntity.ok(adminSystemConfigService.list(pageable));
  }

  @PatchMapping("/{id}")
  @PreAuthorize(CONFIG_WRITE_SECURITY)
  public ResponseEntity<AdminSystemConfigEntryResponse> patch(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("id") long id,
      @Valid @RequestBody UpdateSystemConfigRequest body) {
    String updatedBy = "bank-admin:" + principal.id();
    return ResponseEntity.ok(adminSystemConfigService.updateValue(id, body, updatedBy));
  }
}
