package com.bank.cebos.service.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.integration.SmsIntegration;
import com.bank.cebos.service.auth.PortalPasswordResetNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchServiceTest {

  @Mock private SmsIntegration smsIntegration;
  @Mock private PortalPasswordResetNotifier portalPasswordResetNotifier;

  @Test
  void dispatchesSmsSendToIntegration() throws Exception {
    SmsOutboxEventHandler smsHandler = new SmsOutboxEventHandler(smsIntegration, new ObjectMapper());
    OutboxDispatchService service = new OutboxDispatchService(List.of(smsHandler));
    OutboxEvent event = new OutboxEvent();
    event.setEventType(OutboxEventType.SMS_SEND);
    event.setPayloadJson("{\"toE164\":\"+923001234567\",\"body\":\"Hello\"}");

    service.dispatch(event);

    verify(smsIntegration).sendSms(any());
  }

  @Test
  void dispatchesPortalPasswordResetToNotifier() throws Exception {
    PortalPasswordResetOutboxHandler portalHandler =
        new PortalPasswordResetOutboxHandler(portalPasswordResetNotifier, new ObjectMapper());
    OutboxDispatchService service =
        new OutboxDispatchService(List.of(portalHandler));
    OutboxEvent event = new OutboxEvent();
    event.setEventType(OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL);
    event.setPayloadJson(
        "{\"recipientEmail\":\"u@example.com\",\"rawToken\":\"abc\",\"correlationId\":\"x\"}");

    service.dispatch(event);

    verify(portalPasswordResetNotifier).sendResetLink(eq("u@example.com"), eq("abc"), eq("x"));
  }

  @Test
  void unknownEventTypeDoesNotCallSms() throws Exception {
    SmsOutboxEventHandler smsHandler = new SmsOutboxEventHandler(smsIntegration, new ObjectMapper());
    OutboxDispatchService service = new OutboxDispatchService(List.of(smsHandler));
    OutboxEvent event = new OutboxEvent();
    event.setEventType("UNKNOWN_TYPE");
    event.setPayloadJson("{}");

    service.dispatch(event);

    verifyNoInteractions(smsIntegration);
  }
}
