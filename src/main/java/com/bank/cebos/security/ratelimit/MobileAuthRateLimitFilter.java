package com.bank.cebos.security.ratelimit;

import com.bank.cebos.config.RateLimitProperties;
import com.bank.cebos.security.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis-backed limits for mobile, corporate-portal, and bank-admin auth (plus mobile OTP) per client
 * IP. Fail-open if Redis is down.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MobileAuthRateLimitFilter extends OncePerRequestFilter {

  private static final String PREFIX_MOBILE_LOGIN = "m-login";
  private static final String PREFIX_MOBILE_REFRESH = "m-refresh";
  private static final String PREFIX_MOBILE_OTP = "m-otp";
  private static final String PREFIX_PORTAL_LOGIN = "p-login";
  private static final String PREFIX_PORTAL_REFRESH = "p-refresh";
  private static final String PREFIX_PORTAL_FORGOT = "p-forgot";
  private static final String PREFIX_PORTAL_RESET = "p-reset";
  private static final String PREFIX_ADMIN_LOGIN = "a-login";
  private static final String PREFIX_ADMIN_REFRESH = "a-refresh";

  private final RateLimitProperties rateLimitProperties;
  private final RedisMinuteWindowLimiter limiter;
  private final ObjectMapper objectMapper;

  public MobileAuthRateLimitFilter(
      RateLimitProperties rateLimitProperties,
      RedisMinuteWindowLimiter limiter,
      ObjectMapper objectMapper) {
    this.rateLimitProperties = rateLimitProperties;
    this.limiter = limiter;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    if (!rateLimitProperties.isEnabled()) {
      return true;
    }
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String path = request.getRequestURI();
    return !(path.startsWith("/api/v1/mobile/auth/")
        || path.startsWith("/api/v1/portal/auth/")
        || path.startsWith("/api/v1/admin/auth/"));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    String ip = clientIp(request);
    boolean allowed = true;
    if (path.startsWith("/api/v1/admin/auth/")) {
      if (path.endsWith("/login")) {
        allowed =
            limiter.tryConsume(
                PREFIX_ADMIN_LOGIN, ip, rateLimitProperties.getAdminLoginPerMinute());
      } else if (path.endsWith("/refresh")) {
        allowed =
            limiter.tryConsume(
                PREFIX_ADMIN_REFRESH, ip, rateLimitProperties.getAdminRefreshPerMinute());
      }
    } else if (path.startsWith("/api/v1/portal/auth/")) {
      if (path.endsWith("/login")) {
        allowed =
            limiter.tryConsume(
                PREFIX_PORTAL_LOGIN, ip, rateLimitProperties.getPortalLoginPerMinute());
      } else if (path.endsWith("/refresh")) {
        allowed =
            limiter.tryConsume(
                PREFIX_PORTAL_REFRESH, ip, rateLimitProperties.getPortalRefreshPerMinute());
      } else if (path.endsWith("/forgot-password")) {
        allowed =
            limiter.tryConsume(
                PREFIX_PORTAL_FORGOT,
                ip,
                rateLimitProperties.getPortalForgotPasswordPerMinute());
      } else if (path.endsWith("/reset-password")) {
        allowed =
            limiter.tryConsume(
                PREFIX_PORTAL_RESET, ip, rateLimitProperties.getPortalResetPasswordPerMinute());
      }
    } else if (path.startsWith("/api/v1/mobile/auth/")) {
      if (path.endsWith("/login")) {
        allowed =
            limiter.tryConsume(
                PREFIX_MOBILE_LOGIN, ip, rateLimitProperties.getMobileLoginPerMinute());
      } else if (path.endsWith("/refresh")) {
        allowed =
            limiter.tryConsume(
                PREFIX_MOBILE_REFRESH, ip, rateLimitProperties.getMobileRefreshPerMinute());
      } else if (path.contains("/auth/otp/")) {
        allowed =
            limiter.tryConsume(PREFIX_MOBILE_OTP, ip, rateLimitProperties.getMobileOtpPerMinute());
      }
    }
    if (!allowed) {
      write429(response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
  }

  private void write429(HttpServletResponse response) throws IOException {
    response.setStatus(429);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "Too many requests");
    body.put("status", 429);
    String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
    if (cid != null && !cid.isBlank()) {
      body.put("correlationId", cid);
    }
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
