package com.bank.cebos.service.outbox;

import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.integration.SmsIntegration;
import com.bank.cebos.integration.model.SmsMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SmsOutboxEventHandler implements OutboxEventHandler {

  private final SmsIntegration smsIntegration;
  private final ObjectMapper objectMapper;

  public SmsOutboxEventHandler(SmsIntegration smsIntegration, ObjectMapper objectMapper) {
    this.smsIntegration = smsIntegration;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(String eventType) {
    return OutboxEventType.SMS_SEND.equals(eventType);
  }

  @Override
  public void handle(OutboxEvent event) throws Exception {
    JsonNode root = objectMapper.readTree(event.getPayloadJson());
    String to = root.path("toE164").asText("").trim();
    String body = root.path("body").asText("").trim();
    if (to.isEmpty() || body.isEmpty()) {
      throw new IllegalArgumentException("SMS_SEND payload requires toE164 and body");
    }
    smsIntegration.sendSms(new SmsMessage(to, body));
  }
}
