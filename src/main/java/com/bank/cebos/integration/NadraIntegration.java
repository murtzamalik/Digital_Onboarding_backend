package com.bank.cebos.integration;

import com.bank.cebos.integration.model.NadraVerificationResult;

/** Outbound port for NADRA CNIC verification. */
public interface NadraIntegration {

  NadraVerificationResult verifyByCnic(String cnic);
}
