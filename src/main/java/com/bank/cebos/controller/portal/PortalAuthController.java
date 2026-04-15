package com.bank.cebos.controller.portal;

import com.bank.cebos.dto.auth.ForgotPasswordRequest;
import com.bank.cebos.dto.auth.ForgotPasswordResponse;
import com.bank.cebos.dto.auth.LoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.ResetPasswordRequest;
import com.bank.cebos.dto.auth.ResetPasswordResponse;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.security.CorrelationIdFilter;
import com.bank.cebos.service.auth.PortalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Portal authentication", description = "Corporate portal sign-in and password recovery")
@RestController
@RequestMapping("/api/v1/portal/auth")
public class PortalAuthController {

  private final PortalAuthService portalAuthService;

  public PortalAuthController(PortalAuthService portalAuthService) {
    this.portalAuthService = portalAuthService;
  }

  @Operation(summary = "Sign in", description = "Issue portal access and refresh tokens.")
  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return portalAuthService.login(request);
  }

  @Operation(summary = "Refresh session", description = "Rotate refresh token; returns new access and refresh tokens.")
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return portalAuthService.refresh(request);
  }

  @Operation(
      summary = "Request password reset",
      description =
          "Always returns the same message to avoid email enumeration. Sends reset instructions when a unique active portal user matches the email.")
  @PostMapping("/forgot-password")
  public ForgotPasswordResponse forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
    return portalAuthService.requestPasswordReset(
        request, clientIp(httpRequest), MDC.get(CorrelationIdFilter.MDC_KEY));
  }

  @Operation(
      summary = "Complete password reset",
      description = "Consumes a single-use token from the reset link; revokes existing portal refresh tokens.")
  @PostMapping("/reset-password")
  public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return portalAuthService.resetPassword(request);
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
