package com.bank.cebos.dto.mobile;

import com.bank.cebos.enums.OnboardingStatus;

public record MobileJourneyStatusResponse(
    long employeeOnboardingId, String employeeRef, OnboardingStatus status, Long corporateClientId) {}
