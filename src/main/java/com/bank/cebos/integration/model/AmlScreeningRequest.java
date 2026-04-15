package com.bank.cebos.integration.model;

/** Subject data for AML / sanctions screening. */
public record AmlScreeningRequest(String fullName, String cnic, String nationality) {}
