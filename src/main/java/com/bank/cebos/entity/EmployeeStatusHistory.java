package com.bank.cebos.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "employee_status_history")
@Access(AccessType.FIELD)
public class EmployeeStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_onboarding_id", nullable = false)
  private EmployeeOnboarding employeeOnboarding;

  @Column(name = "from_status", length = 64)
  private String fromStatus;

  @Column(name = "to_status", nullable = false, length = 64)
  private String toStatus;

  @Column(name = "changed_by", nullable = false, length = 256)
  private String changedBy;

  @Column(name = "reason", length = 1024)
  private String reason;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public EmployeeStatusHistory() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public EmployeeOnboarding getEmployeeOnboarding() {
    return employeeOnboarding;
  }

  public void setEmployeeOnboarding(EmployeeOnboarding employeeOnboarding) {
    this.employeeOnboarding = employeeOnboarding;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String fromStatus) {
    this.fromStatus = fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String toStatus) {
    this.toStatus = toStatus;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
