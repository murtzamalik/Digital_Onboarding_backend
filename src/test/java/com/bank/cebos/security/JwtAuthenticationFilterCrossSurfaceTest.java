package com.bank.cebos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.service.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class JwtAuthenticationFilterCrossSurfaceTest {

  private static final String SECRET = "unit-test-jwt-secret-at-least-32-bytes!";

  private JwtTokenService jwtTokenService;
  private MockMvc adminMvc;
  private MockMvc portalMvc;

  @BeforeEach
  void setUp() {
    JwtProperties props = new JwtProperties("cebos-test", 15, 7, SECRET);
    jwtTokenService = new JwtTokenService(props);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService);
    adminMvc =
        MockMvcBuilders.standaloneSetup(new AdminProbeController()).addFilters(filter).build();
    portalMvc =
        MockMvcBuilders.standaloneSetup(new PortalProbeController()).addFilters(filter).build();
  }

  @Test
  void portalTokenRejectedOnAdminRoute() throws Exception {
    String token =
        jwtTokenService.issueAccessToken(
            PrincipalKind.PORTAL, 1L, 99L, "ADMIN", "/api/v1/portal");
    adminMvc
        .perform(
            get("/api/v1/admin/cross-surface-probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminTokenRejectedOnPortalRoute() throws Exception {
    String token =
        jwtTokenService.issueAccessToken(
            PrincipalKind.BANK_ADMIN, 1L, null, "SUPER_ADMIN", "/api/v1/admin");
    portalMvc
        .perform(
            get("/api/v1/portal/cross-surface-probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void mobileTokenRejectedOnPortalRoute() throws Exception {
    String token =
        jwtTokenService.issueAccessToken(PrincipalKind.MOBILE, 1L, 1L, "", "/api/v1/mobile");
    portalMvc
        .perform(
            get("/api/v1/portal/cross-surface-probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @RestController
  @RequestMapping("/api/v1/admin")
  static class AdminProbeController {
    @GetMapping("/cross-surface-probe")
    String probe() {
      return "ok";
    }
  }

  @RestController
  @RequestMapping("/api/v1/portal")
  static class PortalProbeController {
    @GetMapping("/cross-surface-probe")
    String probe() {
      return "ok";
    }
  }
}
