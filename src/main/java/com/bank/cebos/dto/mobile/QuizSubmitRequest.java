package com.bank.cebos.dto.mobile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuizSubmitRequest(
    @NotBlank @Size(max = 64) String templateId, @Valid @NotEmpty List<QuizAnswerRequest> answers) {}
