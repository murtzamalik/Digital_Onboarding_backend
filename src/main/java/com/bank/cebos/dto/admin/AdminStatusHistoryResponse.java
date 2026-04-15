package com.bank.cebos.dto.admin;

import java.time.Instant;

public record AdminStatusHistoryResponse(
    long id,
    String employeeRef,
    String fromStatus,
    String toStatus,
    String changedBy,
    String reason,
    Instant createdAt) {}
