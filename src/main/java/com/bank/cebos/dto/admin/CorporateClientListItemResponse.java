package com.bank.cebos.dto.admin;

public record CorporateClientListItemResponse(
    long id, String publicId, String clientCode, String legalName, String status) {}
