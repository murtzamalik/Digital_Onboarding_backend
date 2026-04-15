package com.bank.cebos.integration.mock;

import com.bank.cebos.integration.NadraIntegration;
import com.bank.cebos.integration.model.NadraVerificationResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!real")
public class MockNadraIntegration implements NadraIntegration {

  @Override
  public NadraVerificationResult verifyByCnic(String cnic) {
    String ref = "MOCK-NADRA-" + refSuffix(cnic);
    return new NadraVerificationResult(true, ref, "MOCK_OK");
  }

  private static String refSuffix(String cnic) {
    return Integer.toHexString(String.valueOf(cnic).hashCode());
  }
}
