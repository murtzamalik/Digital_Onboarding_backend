package com.bank.cebos.integration.real;

import com.bank.cebos.integration.SmsIntegration;
import com.bank.cebos.integration.model.SmsMessage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
public class RealSmsIntegration implements SmsIntegration {

  private static final String MSG = "Real integration is not wired.";

  private final CircuitBreaker circuitBreaker;

  public RealSmsIntegration(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("sms");
  }

  @Override
  public void sendSms(SmsMessage msg) {
    circuitBreaker.executeRunnable(() -> sendSmsInternal(msg));
  }

  private void sendSmsInternal(SmsMessage msg) {
    throw new UnsupportedOperationException(MSG);
  }
}
