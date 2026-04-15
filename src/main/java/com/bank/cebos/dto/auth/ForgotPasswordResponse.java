package com.bank.cebos.dto.auth;

public record ForgotPasswordResponse(String message) {

  public static ForgotPasswordResponse acknowledged() {
    return new ForgotPasswordResponse(
        "If an account exists for this email, password reset instructions have been sent.");
  }
}
