package com.bank.cebos.dto.auth;

public record ResetPasswordResponse(String message) {

  public static ResetPasswordResponse ok() {
    return new ResetPasswordResponse("Password has been updated. You can sign in with the new password.");
  }
}
