package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizAnswerRequest(
    @NotBlank @Size(max = 64) String questionId, @NotBlank @Size(max = 512) String answer) {}
