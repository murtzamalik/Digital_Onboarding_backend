package com.bank.cebos.integration.model;

/** Outcome of an AML screening call. */
public record AmlScreeningResult(
    boolean cleared, String screeningReference, String riskBand) {}
