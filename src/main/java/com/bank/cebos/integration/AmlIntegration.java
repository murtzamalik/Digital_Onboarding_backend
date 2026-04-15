package com.bank.cebos.integration;

import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;

/** Outbound port for AML / sanctions screening. */
public interface AmlIntegration {

  AmlScreeningResult screen(AmlScreeningRequest req);
}
