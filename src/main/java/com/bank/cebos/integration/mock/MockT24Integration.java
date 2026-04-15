package com.bank.cebos.integration.mock;

import com.bank.cebos.integration.T24Integration;
import com.bank.cebos.integration.model.T24AccountOpenCommand;
import com.bank.cebos.integration.model.T24AccountOpenResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!real")
public class MockT24Integration implements T24Integration {

  @Override
  public T24AccountOpenResult openAccount(T24AccountOpenCommand cmd) {
    String cnic = cmd != null ? cmd.cnic() : "";
    String acct = "MOCK-ACC-" + refSuffix(cnic);
    String cust = "MOCK-CUST-" + refSuffix(cnic);
    return new T24AccountOpenResult(true, acct, cust, "mock_open_ok");
  }

  @Override
  public boolean customerExists(String cnic) {
    return cnic != null && !cnic.isBlank();
  }

  private static String refSuffix(String cnic) {
    return Integer.toHexString(String.valueOf(cnic).hashCode());
  }
}
