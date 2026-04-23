package com.bank.cebos.controller.mobile;

import com.bank.cebos.dto.auth.OtpIssueRequest;
import com.bank.cebos.dto.auth.OtpIssueResponse;
import com.bank.cebos.dto.auth.OtpResendRequest;
import com.bank.cebos.dto.auth.OtpResendResponse;
import com.bank.cebos.dto.auth.OtpVerifyRequest;
import com.bank.cebos.dto.auth.OtpVerifyResponse;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.auth.MobileAuthService;
import com.bank.cebos.service.auth.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/mobile/auth/otp")
public class MobileOtpController {

  private final OtpService otpService;
  private final MobileAuthService mobileAuthService;

  public MobileOtpController(OtpService otpService, MobileAuthService mobileAuthService) {
    this.otpService = otpService;
    this.mobileAuthService = mobileAuthService;
  }

  @PostMapping("/issue")
  public OtpIssueResponse issue(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody OtpIssueRequest request) {
    requireMobilePrincipal(principal, request.employeeOnboardingId());
    return new OtpIssueResponse(
        otpService.issueOtp(request.employeeOnboardingId(), request.destinationMasked()));
  }

  @PostMapping("/verify")
  public OtpVerifyResponse verify(@Valid @RequestBody OtpVerifyRequest request) {
    boolean verified = otpService.verifyOtp(request.employeeOnboardingId(), request.otp());
    if (!verified) {
      return new OtpVerifyResponse(false, null, null);
    }
    var tokens = mobileAuthService.issueTokensForEmployeeId(request.employeeOnboardingId());
    return new OtpVerifyResponse(true, tokens.accessToken(), tokens.refreshToken());
  }

  @PostMapping("/resend")
  public OtpResendResponse resend(@Valid @RequestBody OtpResendRequest request) {
    return new OtpResendResponse(otpService.resendOtp(request.employeeOnboardingId()));
  }

  private void requireMobilePrincipal(CebosUserDetails principal, Long employeeOnboardingId) {
    if (principal == null || principal.kind() != PrincipalKind.MOBILE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MOBILE principal required");
    }
    if (principal.id() != employeeOnboardingId) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Principal does not match onboarding context");
    }
  }
}
