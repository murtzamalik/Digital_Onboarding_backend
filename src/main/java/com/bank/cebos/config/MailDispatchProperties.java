package com.bank.cebos.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.mail")
public class MailDispatchProperties {

  /**
   * When true and a {@link org.springframework.mail.javamail.JavaMailSender} bean is available,
   * password-reset notifications are sent via SMTP. When false, notifications are logged only
   * (and still recorded in {@code email_notification_log}).
   */
  private boolean dispatchEnabled = false;

  @NotBlank
  private String fromAddress = "CEBOS <noreply@localhost>";

  public boolean isDispatchEnabled() {
    return dispatchEnabled;
  }

  public void setDispatchEnabled(boolean dispatchEnabled) {
    this.dispatchEnabled = dispatchEnabled;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }
}
