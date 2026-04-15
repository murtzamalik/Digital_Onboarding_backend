package com.bank.cebos.dto.mobile;

import java.util.List;

public record QuizQuestionResponse(String questionId, String prompt, List<String> options) {}
