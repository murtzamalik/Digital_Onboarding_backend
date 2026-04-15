package com.bank.cebos.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "correction_batches")
@Access(AccessType.FIELD)
public class CorrectionBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "corporate_client_id", nullable = false)
  private Long corporateClientId;

  @Column(name = "source_batch_id", nullable = false)
  private Long sourceBatchId;

  @Column(name = "correction_reference", nullable = false, unique = true, length = 64)
  private String correctionReference;

  @Column(name = "original_filename", length = 512)
  private String originalFilename;

  @Column(name = "storage_path", length = 1024)
  private String storagePath;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public CorrectionBatch() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCorporateClientId() {
    return corporateClientId;
  }

  public void setCorporateClientId(Long corporateClientId) {
    this.corporateClientId = corporateClientId;
  }

  public Long getSourceBatchId() {
    return sourceBatchId;
  }

  public void setSourceBatchId(Long sourceBatchId) {
    this.sourceBatchId = sourceBatchId;
  }

  public String getCorrectionReference() {
    return correctionReference;
  }

  public void setCorrectionReference(String correctionReference) {
    this.correctionReference = correctionReference;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public void setStoragePath(String storagePath) {
    this.storagePath = storagePath;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
