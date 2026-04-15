package com.bank.cebos.controller.mobile;

import com.bank.cebos.dto.auth.MobileLoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.service.auth.MobileAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/auth")
public class MobileAuthController {

  private final MobileAuthService mobileAuthService;

  public MobileAuthController(MobileAuthService mobileAuthService) {
    this.mobileAuthService = mobileAuthService;
  }

  @PostMapping("/login")
  public TokenResponse login(
      @Valid @RequestBody MobileLoginRequest request,
      @RequestHeader(value = "X-CEBOS-Mobile-Dev-Secret", required = false) String mobileDevSecret) {
    return mobileAuthService.login(request, mobileDevSecret);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return mobileAuthService.refresh(request);
  }
}
