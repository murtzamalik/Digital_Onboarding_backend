package com.bank.cebos.dto.admin;

import com.bank.cebos.enums.OnboardingStatus;

public record AdminEmployeeSummaryResponse(
    long id,
    String employeeRef,
    OnboardingStatus status,
    Long corporateClientId,
    String fullName,
    String amlScreeningStatus,
    String amlCaseReference) {}
