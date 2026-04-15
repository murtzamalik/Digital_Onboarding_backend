package com.bank.cebos.integration.model;

/** Result of a T24 account opening attempt. */
public record T24AccountOpenResult(
    boolean success, String accountNumber, String t24CustomerId, String message) {}
