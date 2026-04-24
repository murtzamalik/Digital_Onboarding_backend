package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The server loads the ID front image from KYC storage and calls the BBS face-compare API with
 * this selfie. Client does not pass scores or a manual result.
 */
public record FaceMatchSubmitRequest(
    @NotBlank @Size(max = 20_000_000) String selfieBase64) {}
