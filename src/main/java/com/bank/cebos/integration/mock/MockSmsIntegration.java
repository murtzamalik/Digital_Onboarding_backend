package com.bank.cebos.integration.mock;

import com.bank.cebos.integration.SmsIntegration;
import com.bank.cebos.integration.model.SmsMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!real")
public class MockSmsIntegration implements SmsIntegration {

  @Override
  public void sendSms(SmsMessage msg) {
    // Deterministic no-op success; no external call.
  }
}
