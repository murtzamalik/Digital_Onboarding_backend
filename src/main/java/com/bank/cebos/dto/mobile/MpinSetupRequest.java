package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MpinSetupRequest(
    @NotBlank @Pattern(regexp = "\\d{6}", message = "MPIN must be exactly 6 digits") String mpin) {}
