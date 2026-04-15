package com.bank.cebos.entity;

import com.bank.cebos.enums.PrincipalKind;
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
@Table(name = "auth_refresh_tokens")
public class AuthRefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "principal_kind", nullable = false, length = 16)
  private PrincipalKind principalKind;

  @Column(name = "principal_id", nullable = false)
  private long principalId;

  @Column(name = "corporate_client_id")
  private Long corporateClientId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AuthRefreshToken() {}

  public AuthRefreshToken(
      String tokenHash,
      PrincipalKind principalKind,
      long principalId,
      Long corporateClientId,
      Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.principalKind = principalKind;
    this.principalId = principalId;
    this.corporateClientId = corporateClientId;
    this.expiresAt = expiresAt;
    this.revoked = false;
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

  public String getTokenHash() {
    return tokenHash;
  }

  public PrincipalKind getPrincipalKind() {
    return principalKind;
  }

  public long getPrincipalId() {
    return principalId;
  }

  public Long getCorporateClientId() {
    return corporateClientId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
