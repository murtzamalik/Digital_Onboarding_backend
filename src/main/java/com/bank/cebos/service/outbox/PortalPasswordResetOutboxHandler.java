package com.bank.cebos.service.outbox;

import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.service.auth.PortalPasswordResetNotifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class PortalPasswordResetOutboxHandler implements OutboxEventHandler {

  private final PortalPasswordResetNotifier portalPasswordResetNotifier;
  private final ObjectMapper objectMapper;

  public PortalPasswordResetOutboxHandler(
      PortalPasswordResetNotifier portalPasswordResetNotifier, ObjectMapper objectMapper) {
    this.portalPasswordResetNotifier = portalPasswordResetNotifier;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(String eventType) {
    return OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL.equals(eventType);
  }

  @Override
  public void handle(OutboxEvent event) throws Exception {
    JsonNode root = objectMapper.readTree(event.getPayloadJson());
    String to = root.path("recipientEmail").asText("").trim();
    String rawToken = root.path("rawToken").asText("").trim();
    String correlationId = root.path("correlationId").asText("").trim();
    if (to.isEmpty() || rawToken.isEmpty()) {
      throw new IllegalArgumentException(
          "PORTAL_PASSWORD_RESET_EMAIL payload requires recipientEmail and rawToken");
    }
    portalPasswordResetNotifier.sendResetLink(
        to, rawToken, correlationId.isEmpty() ? null : correlationId);
  }
}
