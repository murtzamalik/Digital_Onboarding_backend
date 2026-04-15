package com.bank.cebos.controller.portal;

import com.bank.cebos.dto.portal.PortalNotificationItemResponse;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.portal.PortalNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal/notifications")
public class PortalNotificationController {

  private static final String PORTAL_ROLE_AND_KIND_SECURITY =
      "hasAnyRole('ADMIN','VIEWER') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  private final PortalNotificationService portalNotificationService;

  public PortalNotificationController(PortalNotificationService portalNotificationService) {
    this.portalNotificationService = portalNotificationService;
  }

  @GetMapping
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<Page<PortalNotificationItemResponse>> listNotifications(
      @AuthenticationPrincipal CebosUserDetails principal, Pageable pageable) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.ok(Page.empty(pageable));
    }
    return ResponseEntity.ok(portalNotificationService.listForClient(clientId, pageable));
  }
}
