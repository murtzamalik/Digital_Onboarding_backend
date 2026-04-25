package com.bank.cebos.dto.portal;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Corporate portal read model: sensitive identifiers are masked; KYC document bytes are not
 * embedded (use image endpoint for portal ADMIN).
 */
public record PortalEmployeeDetailResponse(
    String employeeRef,
    String batchReference,
    String status,
    String fullName,
    String fatherName,
    String motherName,
    LocalDate dateOfBirth,
    String gender,
    String religion,
    String mobileMasked,
    String cnicMasked,
    String email,
    LocalDate cnicIssueDate,
    LocalDate cnicExpiryDate,
    String presentAddressLine1,
    String presentAddressLine2,
    String presentCity,
    String presentCountry,
    String permanentAddressLine1,
    String permanentAddressLine2,
    String permanentCity,
    String permanentCountry,
    String amlScreeningStatus,
    String amlCaseReference,
    String t24CustomerId,
    String t24AccountId,
    String t24SubmissionStatus,
    String validationErrors,
    String formDataJson,
    Instant inviteSentAt,
    Instant expireAt,
    Instant createdAt,
    Instant updatedAt,
    boolean hasCnicFrontImage,
    boolean hasCnicBackImage,
    boolean hasSelfieImage) {}
