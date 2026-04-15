package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FingerprintSubmitRequest(
    @NotBlank @Size(max = 256) String templateRef,
    @NotBlank @Size(max = 1024) String capturePath,
    @NotNull @DecimalMin("0.0") BigDecimal qualityScore,
    @NotBlank @Size(max = 32) String result) {}
