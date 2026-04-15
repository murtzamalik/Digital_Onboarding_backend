package com.bank.cebos.dto.admin;

import java.time.Instant;

public record CorporateClientDetailResponse(
    long id,
    String publicId,
    String clientCode,
    String legalName,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
