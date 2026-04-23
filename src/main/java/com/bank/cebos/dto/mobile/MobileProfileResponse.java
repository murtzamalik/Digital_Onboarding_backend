package com.bank.cebos.dto.mobile;

public record MobileProfileResponse(
    Long employeeOnboardingId,
    String employeeRef,
    String status,
    String fullName,
    String fatherName,
    String cnic,
    String mobile,
    String gender,
    String dateOfBirth,
    String cnicIssueDate,
    String cnicExpiryDate,
    String presentAddress,
    String corporateName,
    String designation,
    String sourceOfIncome,
    String purposeOfAccount) {}
