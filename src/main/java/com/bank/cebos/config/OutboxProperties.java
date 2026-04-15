package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.outbox")
public class OutboxProperties {

  private boolean enabled = true;
  /** Polling interval for {@link com.bank.cebos.job.OutboxDispatcherJob} (fixed delay, ms). */
  private int pollIntervalMs = 5000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(int pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }
}
