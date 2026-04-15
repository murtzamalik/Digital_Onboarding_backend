package com.bank.cebos.integration.mock;

import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!real")
public class MockAmlIntegration implements AmlIntegration {

  /**
   * If this marker appears in CNIC or full name, the mock returns a non-cleared hit (tests /
   * demos). Use full name (e.g. {@code AML-HIT Demo}) so CNIC format validation still passes.
   */
  public static final String TRIGGER_UNCLEARED = "AML-HIT";

  @Override
  public AmlScreeningResult screen(AmlScreeningRequest req) {
    String cnic = req != null ? req.cnic() : "";
    String fullName = req != null ? req.fullName() : "";
    if ((cnic != null && cnic.contains(TRIGGER_UNCLEARED))
        || (fullName != null && fullName.contains(TRIGGER_UNCLEARED))) {
      return new AmlScreeningResult(false, "MOCK-AML-HIT", "HIGH");
    }
    String ref = "MOCK-AML-" + Integer.toHexString(String.valueOf(cnic).hashCode());
    return new AmlScreeningResult(true, ref, "LOW");
  }
}
