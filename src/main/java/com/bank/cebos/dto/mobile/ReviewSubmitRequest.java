package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewSubmitRequest(
    @NotBlank @Size(max = 32) String productCode, @NotBlank @Size(max = 3) String currency) {}
