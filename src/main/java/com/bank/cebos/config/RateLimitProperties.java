package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.rate-limit")
public class RateLimitProperties {

  /** When false, rate limiting is skipped (e.g. local unit tests without Redis). */
  private boolean enabled = true;

  /** Max POST /mobile/auth/login per client IP per rolling minute. */
  private int mobileLoginPerMinute = 30;

  /** Max POST /mobile/auth/refresh per client IP per rolling minute. */
  private int mobileRefreshPerMinute = 60;

  /** Max combined OTP issue/verify/resend per client IP per rolling minute. */
  private int mobileOtpPerMinute = 45;

  /** Max POST /portal/auth/login per client IP per rolling minute. */
  private int portalLoginPerMinute = 30;

  /** Max POST /portal/auth/refresh per client IP per rolling minute. */
  private int portalRefreshPerMinute = 60;

  /** Max POST /portal/auth/forgot-password per client IP per rolling minute. */
  private int portalForgotPasswordPerMinute = 10;

  /** Max POST /portal/auth/reset-password per client IP per rolling minute. */
  private int portalResetPasswordPerMinute = 20;

  /** Max POST /admin/auth/login per client IP per rolling minute. */
  private int adminLoginPerMinute = 30;

  /** Max POST /admin/auth/refresh per client IP per rolling minute. */
  private int adminRefreshPerMinute = 60;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMobileLoginPerMinute() {
    return mobileLoginPerMinute;
  }

  public void setMobileLoginPerMinute(int mobileLoginPerMinute) {
    this.mobileLoginPerMinute = mobileLoginPerMinute;
  }

  public int getMobileRefreshPerMinute() {
    return mobileRefreshPerMinute;
  }

  public void setMobileRefreshPerMinute(int mobileRefreshPerMinute) {
    this.mobileRefreshPerMinute = mobileRefreshPerMinute;
  }

  public int getMobileOtpPerMinute() {
    return mobileOtpPerMinute;
  }

  public void setMobileOtpPerMinute(int mobileOtpPerMinute) {
    this.mobileOtpPerMinute = mobileOtpPerMinute;
  }

  public int getPortalLoginPerMinute() {
    return portalLoginPerMinute;
  }

  public void setPortalLoginPerMinute(int portalLoginPerMinute) {
    this.portalLoginPerMinute = portalLoginPerMinute;
  }

  public int getPortalRefreshPerMinute() {
    return portalRefreshPerMinute;
  }

  public void setPortalRefreshPerMinute(int portalRefreshPerMinute) {
    this.portalRefreshPerMinute = portalRefreshPerMinute;
  }

  public int getPortalForgotPasswordPerMinute() {
    return portalForgotPasswordPerMinute;
  }

  public void setPortalForgotPasswordPerMinute(int portalForgotPasswordPerMinute) {
    this.portalForgotPasswordPerMinute = portalForgotPasswordPerMinute;
  }

  public int getPortalResetPasswordPerMinute() {
    return portalResetPasswordPerMinute;
  }

  public void setPortalResetPasswordPerMinute(int portalResetPasswordPerMinute) {
    this.portalResetPasswordPerMinute = portalResetPasswordPerMinute;
  }

  public int getAdminLoginPerMinute() {
    return adminLoginPerMinute;
  }

  public void setAdminLoginPerMinute(int adminLoginPerMinute) {
    this.adminLoginPerMinute = adminLoginPerMinute;
  }

  public int getAdminRefreshPerMinute() {
    return adminRefreshPerMinute;
  }

  public void setAdminRefreshPerMinute(int adminRefreshPerMinute) {
    this.adminRefreshPerMinute = adminRefreshPerMinute;
  }
}
