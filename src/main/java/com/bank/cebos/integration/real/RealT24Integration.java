package com.bank.cebos.integration.real;

import com.bank.cebos.integration.T24Integration;
import com.bank.cebos.integration.model.T24AccountOpenCommand;
import com.bank.cebos.integration.model.T24AccountOpenResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
public class RealT24Integration implements T24Integration {

  private static final String MSG = "Real integration is not wired.";

  private final CircuitBreaker circuitBreaker;

  public RealT24Integration(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("t24");
  }

  @Override
  public T24AccountOpenResult openAccount(T24AccountOpenCommand cmd) {
    return circuitBreaker.executeSupplier(() -> openAccountInternal(cmd));
  }

  @Override
  public boolean customerExists(String cnic) {
    return circuitBreaker.executeSupplier(() -> customerExistsInternal(cnic));
  }

  private T24AccountOpenResult openAccountInternal(T24AccountOpenCommand cmd) {
    throw new UnsupportedOperationException(MSG);
  }

  private boolean customerExistsInternal(String cnic) {
    throw new UnsupportedOperationException(MSG);
  }
}
