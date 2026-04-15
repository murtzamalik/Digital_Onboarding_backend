package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CnicCaptureRequest(@NotBlank @Size(max = 1024) String imagePath) {}
