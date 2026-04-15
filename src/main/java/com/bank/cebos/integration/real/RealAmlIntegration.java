package com.bank.cebos.integration.real;

import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
public class RealAmlIntegration implements AmlIntegration {

  private static final String MSG = "Real integration is not wired.";

  private final CircuitBreaker circuitBreaker;

  public RealAmlIntegration(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aml");
  }

  @Override
  public AmlScreeningResult screen(AmlScreeningRequest req) {
    return circuitBreaker.executeSupplier(() -> screenInternal(req));
  }

  private AmlScreeningResult screenInternal(AmlScreeningRequest req) {
    throw new UnsupportedOperationException(MSG);
  }
}
