package com.bank.cebos.dto.auth;

public record MobileInitResponse(
    Long employeeOnboardingId, String employeeRef, String maskedMobile, String otpEcho) {}
