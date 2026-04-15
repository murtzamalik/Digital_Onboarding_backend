package com.bank.cebos.dto.auth;

import jakarta.validation.constraints.NotNull;

public record OtpResendRequest(@NotNull Long employeeOnboardingId) {}
