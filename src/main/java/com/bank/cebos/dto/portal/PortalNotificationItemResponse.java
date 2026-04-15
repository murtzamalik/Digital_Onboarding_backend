package com.bank.cebos.dto.portal;

import java.time.Instant;

public record PortalNotificationItemResponse(
    long id,
    String templateKey,
    String status,
    String recipientEmail,
    Instant createdAt,
    Instant sentAt,
    String errorMessage) {}
