package com.bank.cebos.dto.portal;

public record PortalCorporateUserResponse(
    long id, String email, String fullName, String role, String status) {}
