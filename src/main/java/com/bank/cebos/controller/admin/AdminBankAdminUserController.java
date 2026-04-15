package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.BankAdminUserResponse;
import com.bank.cebos.dto.admin.CreateBankAdminUserRequest;
import com.bank.cebos.service.admin.AdminBankAdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bank-admin-users")
public class AdminBankAdminUserController {

  private static final String SUPER_ADMIN_ONLY =
      "@principalAccess.hasBankAdminRole(T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN)";

  private final AdminBankAdminUserService adminBankAdminUserService;

  public AdminBankAdminUserController(AdminBankAdminUserService adminBankAdminUserService) {
    this.adminBankAdminUserService = adminBankAdminUserService;
  }

  @GetMapping
  @PreAuthorize(SUPER_ADMIN_ONLY)
  public ResponseEntity<Page<BankAdminUserResponse>> list(Pageable pageable) {
    return ResponseEntity.ok(adminBankAdminUserService.list(pageable));
  }

  @PostMapping
  @PreAuthorize(SUPER_ADMIN_ONLY)
  public ResponseEntity<BankAdminUserResponse> create(@Valid @RequestBody CreateBankAdminUserRequest body) {
    return ResponseEntity.ok(adminBankAdminUserService.create(body));
  }
}
