package com.bank.cebos.service.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.enums.PrincipalKind;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class JwtTokenServiceTest {

  @Test
  void rejectsPortalTokenWhenAdminAudienceExpected() {
    JwtProperties props =
        new JwtProperties("cebos-test", 15, 7, "unit-test-jwt-secret-at-least-32-bytes!");
    JwtTokenService svc = new JwtTokenService(props);
    String token =
        svc.issueAccessToken(PrincipalKind.PORTAL, 1L, 99L, "ROLE_OPS", "/api/v1/portal");
    assertThatThrownBy(() -> svc.parseAndValidateAccessToken(token, "/api/v1/admin"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid token audience");
  }
}
