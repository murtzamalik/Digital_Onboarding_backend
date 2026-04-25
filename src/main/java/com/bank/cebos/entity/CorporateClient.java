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

  @Column(name = "trade_name", length = 256)
  private String tradeName;

  @Column(name = "industry", length = 256)
  private String industry;

  @Column(name = "registered_address", length = 512)
  private String registeredAddress;

  @Column(name = "city", length = 128)
  private String city;

  @Column(name = "contact_phone", length = 64)
  private String contactPhone;

  @Column(name = "contact_email", length = 320)
  private String contactEmail;

  @Column(name = "company_registration_no", length = 128)
  private String companyRegistrationNo;

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

  public String getTradeName() {
    return tradeName;
  }

  public String getIndustry() {
    return industry;
  }

  public String getRegisteredAddress() {
    return registeredAddress;
  }

  public String getCity() {
    return city;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getCompanyRegistrationNo() {
    return companyRegistrationNo;
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

  public void setTradeName(String tradeName) {
    this.tradeName = tradeName;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public void setRegisteredAddress(String registeredAddress) {
    this.registeredAddress = registeredAddress;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public void setCompanyRegistrationNo(String companyRegistrationNo) {
    this.companyRegistrationNo = companyRegistrationNo;
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
