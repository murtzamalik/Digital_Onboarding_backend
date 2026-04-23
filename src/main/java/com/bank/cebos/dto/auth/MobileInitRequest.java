package com.bank.cebos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileInitRequest(@NotBlank @Size(max = 32) String mobile) {}
