package com.bank.cebos.integration.real;

import com.bank.cebos.integration.NadraIntegration;
import com.bank.cebos.integration.model.NadraVerificationResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
public class RealNadraIntegration implements NadraIntegration {

  private static final String MSG = "Real integration is not wired.";

  private final CircuitBreaker circuitBreaker;

  public RealNadraIntegration(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("nadra");
  }

  @Override
  public NadraVerificationResult verifyByCnic(String cnic) {
    return circuitBreaker.executeSupplier(() -> verifyByCnicInternal(cnic));
  }

  private NadraVerificationResult verifyByCnicInternal(String cnic) {
    throw new UnsupportedOperationException(MSG);
  }
}
