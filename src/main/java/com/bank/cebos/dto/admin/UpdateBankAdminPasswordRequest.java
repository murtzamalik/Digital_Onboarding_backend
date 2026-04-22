package com.bank.cebos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBankAdminPasswordRequest(
    @NotBlank @Size(min = 8, max = 128) String newPassword) {}
