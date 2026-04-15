package com.bank.cebos.integration.model;

/** Parameters to open an account in T24. */
public record T24AccountOpenCommand(String cnic, String productCode, String currency) {}
