package com.bank.cebos.dto.portal;

import java.time.Instant;

public record PortalBatchEmployeeRowResponse(
    String employeeRef,
    String status,
    String fullName,
    String mobileMasked,
    String cnicMasked,
    String email,
    Instant inviteSentAt,
    Instant expireAt,
    Instant createdAt,
    Instant updatedAt) {}
