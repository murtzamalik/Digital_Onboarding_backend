package com.bank.cebos.service.outbox;

public final class OutboxEventType {

  public static final String SMS_SEND = "SMS_SEND";

  /** Payload: recipientEmail, rawToken, correlationId (sensitive; process then mark outbox row). */
  public static final String PORTAL_PASSWORD_RESET_EMAIL = "PORTAL_PASSWORD_RESET_EMAIL";

  private OutboxEventType() {}
}
