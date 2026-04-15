package com.bank.cebos.integration.model;

/** Outcome of a CNIC verification against NADRA. */
public record NadraVerificationResult(
    boolean verified, String verificationReference, String statusCode) {}
