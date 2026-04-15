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
@Table(name = "upload_batches")
@Access(AccessType.FIELD)
public class UploadBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "corporate_client_id", nullable = false)
  private Long corporateClientId;

  @Column(name = "uploaded_by_user_id", nullable = false)
  private Long uploadedByUserId;

  @Column(name = "batch_reference", nullable = false, unique = true, length = 64)
  private String batchReference;

  @Column(name = "original_filename", nullable = false, length = 512)
  private String originalFilename;

  @Column(name = "storage_path", length = 1024)
  private String storagePath;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "valid_row_count", nullable = false)
  private int validRowCount;

  @Column(name = "invalid_row_count", nullable = false)
  private int invalidRowCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UploadBatch() {}

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

  public Long getUploadedByUserId() {
    return uploadedByUserId;
  }

  public void setUploadedByUserId(Long uploadedByUserId) {
    this.uploadedByUserId = uploadedByUserId;
  }

  public String getBatchReference() {
    return batchReference;
  }

  public void setBatchReference(String batchReference) {
    this.batchReference = batchReference;
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

  public int getTotalRows() {
    return totalRows;
  }

  public void setTotalRows(int totalRows) {
    this.totalRows = totalRows;
  }

  public int getValidRowCount() {
    return validRowCount;
  }

  public void setValidRowCount(int validRowCount) {
    this.validRowCount = validRowCount;
  }

  public int getInvalidRowCount() {
    return invalidRowCount;
  }

  public void setInvalidRowCount(int invalidRowCount) {
    this.invalidRowCount = invalidRowCount;
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
