package com.bank.cebos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "corporate_users")
public class CorporateUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "corporate_client_id", nullable = false)
  private Long corporateClientId;

  @Column(name = "email", nullable = false, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 512)
  private String fullName;

  @Column(name = "role", nullable = false, length = 32)
  private String role;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  public CorporateUser() {}

  public Long getId() {
    return id;
  }

  public Long getCorporateClientId() {
    return corporateClientId;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public String getRole() {
    return role;
  }

  public String getStatus() {
    return status;
  }

  public void setCorporateClientId(Long corporateClientId) {
    this.corporateClientId = corporateClientId;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
