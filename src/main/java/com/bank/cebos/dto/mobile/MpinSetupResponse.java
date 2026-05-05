package com.bank.cebos.dto.mobile;

import java.util.List;

public record MpinSetupResponse(
    boolean success,
    String message,
    String applicationReference,
    String activationEta,
    List<String> nextSteps) {}
