package com.bank.cebos.integration;

import com.bank.cebos.integration.model.T24AccountOpenCommand;
import com.bank.cebos.integration.model.T24AccountOpenResult;

/** Outbound port for T24 core banking. */
public interface T24Integration {

  T24AccountOpenResult openAccount(T24AccountOpenCommand cmd);

  boolean customerExists(String cnic);
}
