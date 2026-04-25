package com.bank.cebos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCorporateClientRequest(
    @NotBlank @Size(max = 64) String clientCode,
    @NotBlank @Size(max = 512) String legalName,
    @Size(max = 256) String tradeName,
    @Size(max = 256) String industry,
    @Size(max = 512) String registeredAddress,
    @Size(max = 128) String city,
    @Size(max = 64) String contactPhone,
    @Size(max = 320) String contactEmail,
    @Size(max = 128) String companyRegistrationNo) {

  public CreateCorporateClientRequest {
    clientCode = clientCode == null ? "" : clientCode.trim();
    legalName = legalName == null ? "" : legalName.trim();
    tradeName = blankToNull(tradeName);
    industry = blankToNull(industry);
    registeredAddress = blankToNull(registeredAddress);
    city = blankToNull(city);
    contactPhone = blankToNull(contactPhone);
    contactEmail = blankToNull(contactEmail);
    companyRegistrationNo = blankToNull(companyRegistrationNo);
  }

  private static String blankToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
