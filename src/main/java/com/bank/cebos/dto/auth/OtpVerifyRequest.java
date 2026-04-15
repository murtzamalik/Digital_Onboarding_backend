package com.bank.cebos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpVerifyRequest(@NotNull Long employeeOnboardingId, @NotBlank String otp) {}
