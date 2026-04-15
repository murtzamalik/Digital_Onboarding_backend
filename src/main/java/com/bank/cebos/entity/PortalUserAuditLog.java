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
@Table(name = "portal_user_audit_log")
public class PortalUserAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "corporate_user_id")
  private Long corporateUserId;

  @Column(name = "action", nullable = false, length = 128)
  private String action;

  @Column(name = "resource_type", length = 64)
  private String resourceType;

  @Column(name = "resource_id", length = 64)
  private String resourceId;

  @Column(name = "detail_json")
  private String detailJson;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PortalUserAuditLog() {}

  public PortalUserAuditLog(
      Long corporateUserId,
      String action,
      String resourceType,
      String resourceId,
      String detailJson,
      String ipAddress,
      String correlationId) {
    this.corporateUserId = corporateUserId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.detailJson = detailJson;
    this.ipAddress = ipAddress;
    this.correlationId = correlationId;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
