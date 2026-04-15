package com.bank.cebos.integration;

import com.bank.cebos.integration.model.SmsMessage;

/** Outbound port for SMS delivery. */
public interface SmsIntegration {

  void sendSms(SmsMessage msg);
}
