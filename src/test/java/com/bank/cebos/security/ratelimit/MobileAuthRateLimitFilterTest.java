package com.bank.cebos.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class MobileAuthRateLimitFilterTest {

  @Mock private RedisMinuteWindowLimiter limiter;

  private MobileAuthRateLimitFilter filter;

  @BeforeEach
  void setUp() {
    RateLimitProperties props = new RateLimitProperties();
    props.setEnabled(true);
    props.setMobileLoginPerMinute(2);
    props.setMobileRefreshPerMinute(9);
    props.setMobileOtpPerMinute(7);
    props.setPortalLoginPerMinute(5);
    props.setPortalRefreshPerMinute(8);
    props.setAdminLoginPerMinute(4);
    props.setAdminRefreshPerMinute(6);
    filter = new MobileAuthRateLimitFilter(props, limiter, new ObjectMapper());
  }

  @Test
  void returns429WhenMobileOtpLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("m-otp"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/mobile/auth/otp/verify");
    req.setRemoteAddr("203.0.113.9");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    assertThat(res.getContentAsString()).contains("Too many requests");
    verify(limiter).tryConsume(eq("m-otp"), eq("203.0.113.9"), eq(7));
  }

  @Test
  void continuesWhenAllowed() throws ServletException, IOException {
    when(limiter.tryConsume(eq("m-otp"), anyString(), anyInt())).thenReturn(true);
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/mobile/auth/init");
    req.setRemoteAddr("198.51.100.1");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(200);
  }

  @Test
  void usesForwardedForFirstHop() throws ServletException, IOException {
    when(limiter.tryConsume(eq("m-otp"), anyString(), anyInt())).thenReturn(true);
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/mobile/auth/otp/resend");
    req.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    verify(limiter).tryConsume(eq("m-otp"), eq("10.0.0.1"), eq(7));
  }

  @Test
  void skipsPostPathsOutsideAuthEndpoints() throws ServletException, IOException {
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/admin/clients");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    verifyNoInteractions(limiter);
  }

  @Test
  void returns429WhenAdminLoginLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("a-login"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
    req.setRemoteAddr("198.18.0.2");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    verify(limiter).tryConsume(eq("a-login"), eq("198.18.0.2"), eq(4));
  }

  @Test
  void returns429WhenAdminRefreshLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("a-refresh"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/admin/auth/refresh");
    req.setRemoteAddr("198.18.0.3");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    assertThat(res.getContentAsString()).contains("Too many requests");
    verify(limiter).tryConsume(eq("a-refresh"), eq("198.18.0.3"), eq(6));
  }

   @Test
  void returns429WhenPortalLoginLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("p-login"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/portal/auth/login");
    req.setRemoteAddr("198.18.0.1");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    verify(limiter).tryConsume(eq("p-login"), eq("198.18.0.1"), eq(5));
  }

  @Test
  void returns429WhenPortalForgotPasswordLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("p-forgot"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/portal/auth/forgot-password");
    req.setRemoteAddr("198.18.0.10");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    verify(limiter).tryConsume(eq("p-forgot"), eq("198.18.0.10"), eq(10));
  }

  @Test
  void returns429WhenPortalResetPasswordLimited() throws ServletException, IOException {
    when(limiter.tryConsume(eq("p-reset"), anyString(), anyInt())).thenReturn(false);
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/portal/auth/reset-password");
    req.setRemoteAddr("198.18.0.11");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(res.getStatus()).isEqualTo(429);
    verify(limiter).tryConsume(eq("p-reset"), eq("198.18.0.11"), eq(20));
  }
}
