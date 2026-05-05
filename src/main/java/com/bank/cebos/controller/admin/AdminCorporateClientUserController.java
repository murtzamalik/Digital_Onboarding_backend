package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.service.admin.AdminCorporateClientUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/clients/{clientId}/users")
public class AdminCorporateClientUserController {

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  private static final String CLIENT_WRITE_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF)";

  private final AdminCorporateClientUserService adminCorporateClientUserService;

  public AdminCorporateClientUserController(
      AdminCorporateClientUserService adminCorporateClientUserService) {
    this.adminCorporateClientUserService = adminCorporateClientUserService;
  }

  @GetMapping
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<Page<PortalCorporateUserResponse>> listUsers(
      @PathVariable("clientId") long clientId, Pageable pageable) {
    return ResponseEntity.ok(adminCorporateClientUserService.listUsers(clientId, pageable));
  }

  @PostMapping
  @PreAuthorize(CLIENT_WRITE_SECURITY)
  public ResponseEntity<PortalCorporateUserResponse> createUser(
      @PathVariable("clientId") long clientId,
      @Valid @RequestBody PortalCreateCorporateUserRequest request) {
    return ResponseEntity.ok(adminCorporateClientUserService.createUser(clientId, request));
  }
}
