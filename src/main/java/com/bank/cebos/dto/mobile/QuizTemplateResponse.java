package com.bank.cebos.dto.mobile;

import java.util.List;

public record QuizTemplateResponse(
    String templateId, int passingScorePercent, List<QuizQuestionResponse> questions) {}
