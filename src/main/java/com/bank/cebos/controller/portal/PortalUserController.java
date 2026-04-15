package com.bank.cebos.controller.portal;

import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.portal.PortalCorporateUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal/users")
public class PortalUserController {

  private static final String PORTAL_ROLE_AND_KIND_SECURITY =
      "hasAnyRole('ADMIN','VIEWER') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  private static final String PORTAL_ADMIN_SECURITY =
      "hasRole('ADMIN') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  private final PortalCorporateUserService portalCorporateUserService;

  public PortalUserController(PortalCorporateUserService portalCorporateUserService) {
    this.portalCorporateUserService = portalCorporateUserService;
  }

  @GetMapping
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<Page<PortalCorporateUserResponse>> listUsers(
      @AuthenticationPrincipal CebosUserDetails principal, Pageable pageable) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.ok(Page.empty(pageable));
    }
    return ResponseEntity.ok(portalCorporateUserService.listForClient(clientId, pageable));
  }

  @PostMapping
  @PreAuthorize(PORTAL_ADMIN_SECURITY)
  public ResponseEntity<PortalCorporateUserResponse> createUser(
      @AuthenticationPrincipal CebosUserDetails principal,
      @Valid @RequestBody PortalCreateCorporateUserRequest request) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.badRequest().build();
    }
    PortalCorporateUserResponse body = portalCorporateUserService.createForClient(clientId, request);
    return ResponseEntity.ok(body);
  }
}
