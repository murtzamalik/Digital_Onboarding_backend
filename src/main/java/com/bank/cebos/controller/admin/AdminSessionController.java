package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.AdminSessionResponse;
import com.bank.cebos.security.CebosUserDetails;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSessionController {

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  @GetMapping("/session")
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<AdminSessionResponse> session(
      @AuthenticationPrincipal CebosUserDetails principal) {
    List<String> authorities =
        principal.authorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    return ResponseEntity.ok(new AdminSessionResponse(principal.id(), authorities));
  }
}
