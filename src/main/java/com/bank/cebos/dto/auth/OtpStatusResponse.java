package com.bank.cebos.dto.auth;

public record OtpStatusResponse(
    int attemptsUsed,
    int maxAttempts,
    int attemptsRemaining,
    int resendAvailableInSeconds,
    boolean locked,
    int lockoutRemainingSeconds) {}
