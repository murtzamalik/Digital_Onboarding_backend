package com.bank.cebos.controller.mobile;

import com.bank.cebos.dto.auth.MobileInitRequest;
import com.bank.cebos.dto.auth.MobileInitResponse;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.service.auth.MobileAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/auth")
public class MobileAuthController {

  private final MobileAuthService mobileAuthService;

  public MobileAuthController(MobileAuthService mobileAuthService) {
    this.mobileAuthService = mobileAuthService;
  }

  @PostMapping("/init")
  public MobileInitResponse init(@Valid @RequestBody MobileInitRequest request) {
    return mobileAuthService.initByMobile(request.mobile());
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return mobileAuthService.refresh(request);
  }
}
