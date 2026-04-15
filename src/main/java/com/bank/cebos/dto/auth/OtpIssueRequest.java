package com.bank.cebos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpIssueRequest(@NotNull Long employeeOnboardingId, @NotBlank String destinationMasked) {}
