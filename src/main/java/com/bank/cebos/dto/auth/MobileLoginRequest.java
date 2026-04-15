package com.bank.cebos.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record MobileLoginRequest(@NotBlank String employeeRef) {}
