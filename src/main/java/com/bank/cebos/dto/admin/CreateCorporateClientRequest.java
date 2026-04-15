package com.bank.cebos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCorporateClientRequest(
    @NotBlank @Size(max = 64) String clientCode, @NotBlank @Size(max = 512) String legalName) {}
