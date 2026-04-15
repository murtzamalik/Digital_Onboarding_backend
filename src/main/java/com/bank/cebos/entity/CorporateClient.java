package com.bank.cebos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "corporate_clients")
public class CorporateClient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, length = 36, unique = true)
  private String publicId;

  @Column(name = "client_code", nullable = false, length = 64, unique = true)
  private String clientCode;

  @Column(name = "legal_name", nullable = false, length = 512)
  private String legalName;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public CorporateClient() {}

  public Long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getLegalName() {
    return legalName;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public void setClientCode(String clientCode) {
    this.clientCode = clientCode;
  }

  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
