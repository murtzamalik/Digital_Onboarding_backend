package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.jobs")
public class JobsProperties {

  private JobSchedule otpExpiry = new JobSchedule();
  private JobSchedule inviteExpiry = new JobSchedule();
  private JobSchedule uploadedValidation = new JobSchedule();
  private JobSchedule inviteDispatch = new JobSchedule();
  private JobSchedule t24Retry = new JobSchedule();
  private JobSchedule slaAlert = new JobSchedule();
  private int uploadedValidationBatchSize = 100;
  private int inviteDispatchBatchSize = 50;
  private int slaAlertDays = 3;
  private int t24RetryAttemptsPlaceholder = 3;
  private int t24RetryBatchSize = 50;

  public JobSchedule getOtpExpiry() {
    return otpExpiry;
  }

  public void setOtpExpiry(JobSchedule otpExpiry) {
    this.otpExpiry = otpExpiry;
  }

  public JobSchedule getInviteExpiry() {
    return inviteExpiry;
  }

  public void setInviteExpiry(JobSchedule inviteExpiry) {
    this.inviteExpiry = inviteExpiry;
  }

  public JobSchedule getUploadedValidation() {
    return uploadedValidation;
  }

  public void setUploadedValidation(JobSchedule uploadedValidation) {
    this.uploadedValidation = uploadedValidation;
  }

  public JobSchedule getInviteDispatch() {
    return inviteDispatch;
  }

  public void setInviteDispatch(JobSchedule inviteDispatch) {
    this.inviteDispatch = inviteDispatch;
  }

  public int getUploadedValidationBatchSize() {
    return uploadedValidationBatchSize;
  }

  public void setUploadedValidationBatchSize(int uploadedValidationBatchSize) {
    this.uploadedValidationBatchSize = uploadedValidationBatchSize;
  }

  public int getInviteDispatchBatchSize() {
    return inviteDispatchBatchSize;
  }

  public void setInviteDispatchBatchSize(int inviteDispatchBatchSize) {
    this.inviteDispatchBatchSize = inviteDispatchBatchSize;
  }

  public JobSchedule getT24Retry() {
    return t24Retry;
  }

  public void setT24Retry(JobSchedule t24Retry) {
    this.t24Retry = t24Retry;
  }

  public JobSchedule getSlaAlert() {
    return slaAlert;
  }

  public void setSlaAlert(JobSchedule slaAlert) {
    this.slaAlert = slaAlert;
  }

  public int getSlaAlertDays() {
    return slaAlertDays;
  }

  public void setSlaAlertDays(int slaAlertDays) {
    this.slaAlertDays = slaAlertDays;
  }

  public int getT24RetryAttemptsPlaceholder() {
    return t24RetryAttemptsPlaceholder;
  }

  public void setT24RetryAttemptsPlaceholder(int t24RetryAttemptsPlaceholder) {
    this.t24RetryAttemptsPlaceholder = t24RetryAttemptsPlaceholder;
  }

  public int getT24RetryBatchSize() {
    return t24RetryBatchSize;
  }

  public void setT24RetryBatchSize(int t24RetryBatchSize) {
    this.t24RetryBatchSize = t24RetryBatchSize;
  }

  public static class JobSchedule {
    private boolean enabled = true;
    private String cron = "*/30 * * * * *";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }
  }
}
