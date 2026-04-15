package com.bank.cebos.service.outbox;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.service.auth.PortalPasswordResetNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalPasswordResetOutboxHandlerTest {

  @Mock private PortalPasswordResetNotifier portalPasswordResetNotifier;

  @Test
  void invokesNotifierWithPayloadFields() throws Exception {
    PortalPasswordResetOutboxHandler handler =
        new PortalPasswordResetOutboxHandler(portalPasswordResetNotifier, new ObjectMapper());
    OutboxEvent event = new OutboxEvent();
    event.setEventType(OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL);
    event.setPayloadJson(
        "{\"recipientEmail\":\"a@b.com\",\"rawToken\":\"tok123\",\"correlationId\":\"c1\"}");

    handler.handle(event);

    verify(portalPasswordResetNotifier).sendResetLink(eq("a@b.com"), eq("tok123"), eq("c1"));
  }

  @Test
  void supportsPortalPasswordResetType() {
    PortalPasswordResetOutboxHandler handler =
        new PortalPasswordResetOutboxHandler(portalPasswordResetNotifier, new ObjectMapper());
    org.assertj.core.api.Assertions.assertThat(handler.supports(OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(handler.supports(OutboxEventType.SMS_SEND)).isFalse();
  }
}
