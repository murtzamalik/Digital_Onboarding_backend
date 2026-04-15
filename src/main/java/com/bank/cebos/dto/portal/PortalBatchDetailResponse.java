package com.bank.cebos.dto.portal;

import java.time.Instant;

public record PortalBatchDetailResponse(
    long id,
    String batchReference,
    String originalFilename,
    String status,
    int totalRows,
    int validRowCount,
    int invalidRowCount,
    Instant createdAt,
    Instant updatedAt) {}
