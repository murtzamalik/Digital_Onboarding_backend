package com.bank.cebos.entity;

import com.bank.cebos.enums.OtpSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "otp_sessions")
public class OtpSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_onboarding_id", nullable = false)
  private Long employeeOnboardingId;

  @Column(name = "channel", nullable = false, length = 32)
  private String channel;

  @Column(name = "destination_masked", length = 64)
  private String destinationMasked;

  @Column(name = "otp_hash", nullable = false, length = 255)
  private String otpHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "attempt_count", nullable = false)
  private Integer attemptCount;

  @Column(name = "max_attempts", nullable = false)
  private Integer maxAttempts;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private OtpSessionStatus status;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEmployeeOnboardingId() {
    return employeeOnboardingId;
  }

  public void setEmployeeOnboardingId(Long employeeOnboardingId) {
    this.employeeOnboardingId = employeeOnboardingId;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getDestinationMasked() {
    return destinationMasked;
  }

  public void setDestinationMasked(String destinationMasked) {
    this.destinationMasked = destinationMasked;
  }

  public String getOtpHash() {
    return otpHash;
  }

  public void setOtpHash(String otpHash) {
    this.otpHash = otpHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Integer getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Integer attemptCount) {
    this.attemptCount = attemptCount;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(Instant verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public OtpSessionStatus getStatus() {
    return status;
  }

  public void setStatus(OtpSessionStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (attemptCount == null) {
      attemptCount = 0;
    }
    if (status == null) {
      status = OtpSessionStatus.ACTIVE;
    }
  }
}
