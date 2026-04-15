package com.bank.cebos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmlComplianceReasonRequest(@NotBlank @Size(max = 1024) String reason) {}
