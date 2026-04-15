package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.auth.LoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.service.auth.BankAdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class BankAdminAuthController {

  private final BankAdminAuthService bankAdminAuthService;

  public BankAdminAuthController(BankAdminAuthService bankAdminAuthService) {
    this.bankAdminAuthService = bankAdminAuthService;
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return bankAdminAuthService.login(request);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return bankAdminAuthService.refresh(request);
  }
}
