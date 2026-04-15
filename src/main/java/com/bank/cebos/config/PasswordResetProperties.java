package com.bank.cebos.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.password-reset")
public class PasswordResetProperties {

  @Min(1)
  @Max(168)
  private int tokenValidityHours = 24;

  /** Public corporate portal origin (no trailing slash), used to build reset links in notifications. */
  @NotBlank
  private String publicPortalBaseUrl = "http://localhost:5173";

  public int getTokenValidityHours() {
    return tokenValidityHours;
  }

  public void setTokenValidityHours(int tokenValidityHours) {
    this.tokenValidityHours = tokenValidityHours;
  }

  public String getPublicPortalBaseUrl() {
    return publicPortalBaseUrl;
  }

  public void setPublicPortalBaseUrl(String publicPortalBaseUrl) {
    this.publicPortalBaseUrl = publicPortalBaseUrl;
  }
}
