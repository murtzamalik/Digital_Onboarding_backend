package com.bank.cebos.integration.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import org.junit.jupiter.api.Test;

class MockAmlIntegrationTest {

  private final MockAmlIntegration aml = new MockAmlIntegration();

  @Test
  void defaultRequestClears() {
    AmlScreeningResult r =
        aml.screen(new AmlScreeningRequest("Test User", "11111-1111111-1", "PK"));
    assertThat(r.cleared()).isTrue();
    assertThat(r.riskBand()).isEqualTo("LOW");
  }

  @Test
  void fullNameWithTriggerDoesNotClear() {
    AmlScreeningResult r =
        aml.screen(
            new AmlScreeningRequest(
                "Demo " + MockAmlIntegration.TRIGGER_UNCLEARED + " User",
                "11111-1111111-1",
                "PK"));
    assertThat(r.cleared()).isFalse();
    assertThat(r.screeningReference()).isEqualTo("MOCK-AML-HIT");
    assertThat(r.riskBand()).isEqualTo("HIGH");
  }
}
