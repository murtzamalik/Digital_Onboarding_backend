package com.bank.cebos.controller.portal;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
public class PortalPingController {

  @GetMapping("/ping")
  @PreAuthorize(
      "@principalAccess.hasPortalRole(T(com.bank.cebos.enums.PortalRole).ADMIN, T(com.bank.cebos.enums.PortalRole).VIEWER)")
  public ResponseEntity<Map<String, String>> ping() {
    return ResponseEntity.ok(Map.of("message", "pong"));
  }
}
