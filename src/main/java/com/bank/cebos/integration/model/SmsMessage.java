package com.bank.cebos.integration.model;

/** Outbound SMS payload. */
public record SmsMessage(String toE164, String body) {}
