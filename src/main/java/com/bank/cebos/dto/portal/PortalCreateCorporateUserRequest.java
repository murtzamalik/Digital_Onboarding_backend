package com.bank.cebos.dto.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortalCreateCorporateUserRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 512) String fullName,
    @NotBlank @Size(max = 32) String role) {}
