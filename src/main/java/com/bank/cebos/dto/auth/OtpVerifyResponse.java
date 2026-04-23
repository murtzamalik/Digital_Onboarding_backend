package com.bank.cebos.dto.auth;

public record OtpVerifyResponse(boolean verified, String accessToken, String refreshToken) {}
