package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Multipart alternative is possible later; the mobile app sends a single base64 (optionally
 * data-URL) JPEG/PNG, and the backend calls BBS Pakistani CNIC OCR.
 */
public record CnicCaptureRequest(
    @NotBlank @Size(max = 20_000_000) String base64Image) {}
