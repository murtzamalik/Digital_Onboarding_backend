package com.bank.cebos.controller.portal;

import com.bank.cebos.dto.portal.PortalSessionResponse;
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
@RequestMapping("/api/v1/portal")
public class PortalSessionController {

  private static final String PORTAL_USER =
      "hasAnyRole('ADMIN','VIEWER') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  @GetMapping("/session")
  @PreAuthorize(PORTAL_USER)
  public ResponseEntity<PortalSessionResponse> session(
      @AuthenticationPrincipal CebosUserDetails principal) {
    List<String> authorities =
        principal.authorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    return ResponseEntity.ok(
        new PortalSessionResponse(principal.id(), principal.corporateClientId(), authorities));
  }
}
