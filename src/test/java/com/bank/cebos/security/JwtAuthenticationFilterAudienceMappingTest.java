package com.bank.cebos.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JwtAuthenticationFilterAudienceMappingTest {

  @Test
  void resolveExpectedAudienceMapsKnownSurfaces() {
    assertEquals("/api/v1/portal", JwtAuthenticationFilter.resolveExpectedAudience("/api/v1/portal/ping"));
    assertEquals("/api/v1/mobile", JwtAuthenticationFilter.resolveExpectedAudience("/api/v1/mobile/status"));
    assertEquals("/api/v1/admin", JwtAuthenticationFilter.resolveExpectedAudience("/api/v1/admin/ping"));
  }

  @Test
  void resolveExpectedAudienceReturnsNullForUnsupportedUris() {
    assertNull(JwtAuthenticationFilter.resolveExpectedAudience("/api/v2/portal/ping"));
    assertNull(JwtAuthenticationFilter.resolveExpectedAudience("/actuator/health"));
    assertNull(JwtAuthenticationFilter.resolveExpectedAudience(null));
  }
}
