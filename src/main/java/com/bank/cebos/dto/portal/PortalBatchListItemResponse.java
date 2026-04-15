package com.bank.cebos.dto.portal;

import java.time.Instant;

public record PortalBatchListItemResponse(
    long id,
    String batchReference,
    String status,
    int totalRows,
    int validRowCount,
    int invalidRowCount,
    Instant createdAt) {}
