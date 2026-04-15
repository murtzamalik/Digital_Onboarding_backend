package com.bank.cebos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "portal_password_reset_tokens")
public class PortalPasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "corporate_user_id", nullable = false)
  private Long corporateUserId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "request_ip", length = 64)
  private String requestIp;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PortalPasswordResetToken() {}

  public PortalPasswordResetToken(
      Long corporateUserId,
      String tokenHash,
      Instant expiresAt,
      String requestIp,
      String correlationId) {
    this.corporateUserId = corporateUserId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.requestIp = requestIp;
    this.correlationId = correlationId;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getCorporateUserId() {
    return corporateUserId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }
}
