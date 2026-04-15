package com.bank.cebos.dto.admin;

import java.time.Instant;

public record AdminSystemConfigEntryResponse(
    long id,
    String configKey,
    String configValue,
    String valueType,
    String description,
    String updatedBy,
    Instant updatedAt) {}
