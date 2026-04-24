package com.bank.cebos.entity;

import com.bank.cebos.enums.OnboardingStatus;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employee_onboarding")
@Access(AccessType.FIELD)
public class EmployeeOnboarding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_ref", nullable = false, unique = true, length = 64)
  private String employeeRef;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "correction_batch_id")
  private Long correctionBatchId;

  @Column(name = "corporate_client_id", nullable = false)
  private Long corporateClientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 64)
  private OnboardingStatus status;

  @Column(name = "block_reason", length = 512)
  private String blockReason;

  @Column(name = "blocked_at")
  private Instant blockedAt;

  @Column(name = "unblocked_by", length = 256)
  private String unblockedBy;

  @Column(name = "unblocked_at")
  private Instant unblockedAt;

  @Column(name = "cnic", length = 32)
  private String cnic;

  @Column(name = "mobile", length = 32)
  private String mobile;

  @Column(name = "email", length = 320)
  private String email;

  @Column(name = "full_name", length = 512)
  private String fullName;

  @Column(name = "father_name", length = 512)
  private String fatherName;

  @Column(name = "mother_name", length = 512)
  private String motherName;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "gender", length = 32)
  private String gender;

  @Column(name = "religion", length = 128)
  private String religion;

  @Column(name = "cnic_issue_date")
  private LocalDate cnicIssueDate;

  @Column(name = "cnic_expiry_date")
  private LocalDate cnicExpiryDate;

  @Column(name = "present_address_line1", length = 512)
  private String presentAddressLine1;

  @Column(name = "present_address_line2", length = 512)
  private String presentAddressLine2;

  @Column(name = "present_city", length = 128)
  private String presentCity;

  @Column(name = "present_country", length = 128)
  private String presentCountry;

  @Column(name = "permanent_address_line1", length = 512)
  private String permanentAddressLine1;

  @Column(name = "permanent_address_line2", length = 512)
  private String permanentAddressLine2;

  @Column(name = "permanent_city", length = 128)
  private String permanentCity;

  @Column(name = "permanent_country", length = 128)
  private String permanentCountry;

  @Column(name = "nadra_transaction_id", length = 128)
  private String nadraTransactionId;

  @Column(name = "nadra_verification_status", length = 64)
  private String nadraVerificationStatus;

  @Column(name = "nadra_verification_code", length = 64)
  private String nadraVerificationCode;

  @Lob
  @Column(name = "nadra_response_payload")
  private String nadraResponsePayload;

  @Column(name = "nadra_verified_at")
  private Instant nadraVerifiedAt;

  @Column(name = "cnic_front_image_path", length = 1024)
  private String cnicFrontImagePath;

  @Column(name = "cnic_back_image_path", length = 1024)
  private String cnicBackImagePath;

  @Column(name = "selfie_image_path", length = 1024)
  private String selfieImagePath;

  @Lob
  @Column(name = "cnic_front_image_data", columnDefinition = "LONGBLOB")
  private byte[] cnicFrontImageData;

  @Lob
  @Column(name = "cnic_back_image_data", columnDefinition = "LONGBLOB")
  private byte[] cnicBackImageData;

  @Lob
  @Column(name = "selfie_image_data", columnDefinition = "LONGBLOB")
  private byte[] selfieImageData;

  @Column(name = "liveness_session_id", length = 128)
  private String livenessSessionId;

  @Column(name = "liveness_vendor_ref", length = 256)
  private String livenessVendorRef;

  @Column(name = "liveness_score", precision = 7, scale = 4)
  private BigDecimal livenessScore;

  @Column(name = "liveness_result", length = 32)
  private String livenessResult;

  @Column(name = "liveness_completed_at")
  private Instant livenessCompletedAt;

  @Column(name = "face_match_score", precision = 7, scale = 4)
  private BigDecimal faceMatchScore;

  @Column(name = "face_match_result", length = 32)
  private String faceMatchResult;

  @Column(name = "face_match_completed_at")
  private Instant faceMatchCompletedAt;

  @Column(name = "fingerprint_template_ref", length = 256)
  private String fingerprintTemplateRef;

  @Column(name = "fingerprint_capture_path", length = 1024)
  private String fingerprintCapturePath;

  @Column(name = "fingerprint_quality_score", precision = 7, scale = 4)
  private BigDecimal fingerprintQualityScore;

  @Column(name = "fingerprint_match_result", length = 32)
  private String fingerprintMatchResult;

  @Column(name = "fingerprint_completed_at")
  private Instant fingerprintCompletedAt;

  @Column(name = "quiz_template_id", length = 64)
  private String quizTemplateId;

  @Column(name = "quiz_score")
  private Integer quizScore;

  @Column(name = "quiz_max_score")
  private Integer quizMaxScore;

  @Column(name = "quiz_passed")
  private Boolean quizPassed;

  @Lob
  @Column(name = "quiz_answers_json")
  private String quizAnswersJson;

  @Column(name = "quiz_completed_at")
  private Instant quizCompletedAt;

  @Column(name = "mpin_hash", length = 256)
  private String mpinHash;

  @Lob
  @Column(name = "form_data_json")
  private String formDataJson;

  @Column(name = "form_submitted_at")
  private Instant formSubmittedAt;

  @Column(name = "aml_case_reference", length = 128)
  private String amlCaseReference;

  @Column(name = "aml_screening_status", length = 64)
  private String amlScreeningStatus;

  @Lob
  @Column(name = "aml_screening_summary")
  private String amlScreeningSummary;

  @Column(name = "aml_last_checked_at")
  private Instant amlLastCheckedAt;

  @Column(name = "t24_customer_id", length = 64)
  private String t24CustomerId;

  @Column(name = "t24_account_id", length = 64)
  private String t24AccountId;

  @Column(name = "t24_submission_status", length = 64)
  private String t24SubmissionStatus;

  @Lob
  @Column(name = "t24_last_error")
  private String t24LastError;

  @Column(name = "t24_last_attempt_at")
  private Instant t24LastAttemptAt;

  @Column(name = "ocr_job_id", length = 128)
  private String ocrJobId;

  @Column(name = "ocr_status", length = 64)
  private String ocrStatus;

  @Lob
  @Column(name = "ocr_extracted_json")
  private String ocrExtractedJson;

  @Lob
  @Column(name = "validation_errors")
  private String validationErrors;

  @Column(name = "expire_at")
  private Instant expireAt;

  @Column(name = "invite_sent_at")
  private Instant inviteSentAt;

  @Column(name = "invite_resend_count", nullable = false)
  private Integer inviteResendCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public EmployeeOnboarding() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmployeeRef() {
    return employeeRef;
  }

  public void setEmployeeRef(String employeeRef) {
    this.employeeRef = employeeRef;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public Long getCorrectionBatchId() {
    return correctionBatchId;
  }

  public void setCorrectionBatchId(Long correctionBatchId) {
    this.correctionBatchId = correctionBatchId;
  }

  public Long getCorporateClientId() {
    return corporateClientId;
  }

  public void setCorporateClientId(Long corporateClientId) {
    this.corporateClientId = corporateClientId;
  }

  public OnboardingStatus getStatus() {
    return status;
  }

  public void setStatus(OnboardingStatus status) {
    this.status = status;
  }

  public String getBlockReason() {
    return blockReason;
  }

  public void setBlockReason(String blockReason) {
    this.blockReason = blockReason;
  }

  public Instant getBlockedAt() {
    return blockedAt;
  }

  public void setBlockedAt(Instant blockedAt) {
    this.blockedAt = blockedAt;
  }

  public String getUnblockedBy() {
    return unblockedBy;
  }

  public void setUnblockedBy(String unblockedBy) {
    this.unblockedBy = unblockedBy;
  }

  public Instant getUnblockedAt() {
    return unblockedAt;
  }

  public void setUnblockedAt(Instant unblockedAt) {
    this.unblockedAt = unblockedAt;
  }

  public String getCnic() {
    return cnic;
  }

  public void setCnic(String cnic) {
    this.cnic = cnic;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFatherName() {
    return fatherName;
  }

  public void setFatherName(String fatherName) {
    this.fatherName = fatherName;
  }

  public String getMotherName() {
    return motherName;
  }

  public void setMotherName(String motherName) {
    this.motherName = motherName;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getReligion() {
    return religion;
  }

  public void setReligion(String religion) {
    this.religion = religion;
  }

  public LocalDate getCnicIssueDate() {
    return cnicIssueDate;
  }

  public void setCnicIssueDate(LocalDate cnicIssueDate) {
    this.cnicIssueDate = cnicIssueDate;
  }

  public LocalDate getCnicExpiryDate() {
    return cnicExpiryDate;
  }

  public void setCnicExpiryDate(LocalDate cnicExpiryDate) {
    this.cnicExpiryDate = cnicExpiryDate;
  }

  public String getPresentAddressLine1() {
    return presentAddressLine1;
  }

  public void setPresentAddressLine1(String presentAddressLine1) {
    this.presentAddressLine1 = presentAddressLine1;
  }

  public String getPresentAddressLine2() {
    return presentAddressLine2;
  }

  public void setPresentAddressLine2(String presentAddressLine2) {
    this.presentAddressLine2 = presentAddressLine2;
  }

  public String getPresentCity() {
    return presentCity;
  }

  public void setPresentCity(String presentCity) {
    this.presentCity = presentCity;
  }

  public String getPresentCountry() {
    return presentCountry;
  }

  public void setPresentCountry(String presentCountry) {
    this.presentCountry = presentCountry;
  }

  public String getPermanentAddressLine1() {
    return permanentAddressLine1;
  }

  public void setPermanentAddressLine1(String permanentAddressLine1) {
    this.permanentAddressLine1 = permanentAddressLine1;
  }

  public String getPermanentAddressLine2() {
    return permanentAddressLine2;
  }

  public void setPermanentAddressLine2(String permanentAddressLine2) {
    this.permanentAddressLine2 = permanentAddressLine2;
  }

  public String getPermanentCity() {
    return permanentCity;
  }

  public void setPermanentCity(String permanentCity) {
    this.permanentCity = permanentCity;
  }

  public String getPermanentCountry() {
    return permanentCountry;
  }

  public void setPermanentCountry(String permanentCountry) {
    this.permanentCountry = permanentCountry;
  }

  public String getNadraTransactionId() {
    return nadraTransactionId;
  }

  public void setNadraTransactionId(String nadraTransactionId) {
    this.nadraTransactionId = nadraTransactionId;
  }

  public String getNadraVerificationStatus() {
    return nadraVerificationStatus;
  }

  public void setNadraVerificationStatus(String nadraVerificationStatus) {
    this.nadraVerificationStatus = nadraVerificationStatus;
  }

  public String getNadraVerificationCode() {
    return nadraVerificationCode;
  }

  public void setNadraVerificationCode(String nadraVerificationCode) {
    this.nadraVerificationCode = nadraVerificationCode;
  }

  public String getNadraResponsePayload() {
    return nadraResponsePayload;
  }

  public void setNadraResponsePayload(String nadraResponsePayload) {
    this.nadraResponsePayload = nadraResponsePayload;
  }

  public Instant getNadraVerifiedAt() {
    return nadraVerifiedAt;
  }

  public void setNadraVerifiedAt(Instant nadraVerifiedAt) {
    this.nadraVerifiedAt = nadraVerifiedAt;
  }

  public String getCnicFrontImagePath() {
    return cnicFrontImagePath;
  }

  public void setCnicFrontImagePath(String cnicFrontImagePath) {
    this.cnicFrontImagePath = cnicFrontImagePath;
  }

  public String getCnicBackImagePath() {
    return cnicBackImagePath;
  }

  public void setCnicBackImagePath(String cnicBackImagePath) {
    this.cnicBackImagePath = cnicBackImagePath;
  }

  public String getSelfieImagePath() {
    return selfieImagePath;
  }

  public void setSelfieImagePath(String selfieImagePath) {
    this.selfieImagePath = selfieImagePath;
  }

  public byte[] getCnicFrontImageData() {
    return cnicFrontImageData;
  }

  public void setCnicFrontImageData(byte[] cnicFrontImageData) {
    this.cnicFrontImageData = cnicFrontImageData;
  }

  public byte[] getCnicBackImageData() {
    return cnicBackImageData;
  }

  public void setCnicBackImageData(byte[] cnicBackImageData) {
    this.cnicBackImageData = cnicBackImageData;
  }

  public byte[] getSelfieImageData() {
    return selfieImageData;
  }

  public void setSelfieImageData(byte[] selfieImageData) {
    this.selfieImageData = selfieImageData;
  }

  public String getLivenessSessionId() {
    return livenessSessionId;
  }

  public void setLivenessSessionId(String livenessSessionId) {
    this.livenessSessionId = livenessSessionId;
  }

  public String getLivenessVendorRef() {
    return livenessVendorRef;
  }

  public void setLivenessVendorRef(String livenessVendorRef) {
    this.livenessVendorRef = livenessVendorRef;
  }

  public BigDecimal getLivenessScore() {
    return livenessScore;
  }

  public void setLivenessScore(BigDecimal livenessScore) {
    this.livenessScore = livenessScore;
  }

  public String getLivenessResult() {
    return livenessResult;
  }

  public void setLivenessResult(String livenessResult) {
    this.livenessResult = livenessResult;
  }

  public Instant getLivenessCompletedAt() {
    return livenessCompletedAt;
  }

  public void setLivenessCompletedAt(Instant livenessCompletedAt) {
    this.livenessCompletedAt = livenessCompletedAt;
  }

  public BigDecimal getFaceMatchScore() {
    return faceMatchScore;
  }

  public void setFaceMatchScore(BigDecimal faceMatchScore) {
    this.faceMatchScore = faceMatchScore;
  }

  public String getFaceMatchResult() {
    return faceMatchResult;
  }

  public void setFaceMatchResult(String faceMatchResult) {
    this.faceMatchResult = faceMatchResult;
  }

  public Instant getFaceMatchCompletedAt() {
    return faceMatchCompletedAt;
  }

  public void setFaceMatchCompletedAt(Instant faceMatchCompletedAt) {
    this.faceMatchCompletedAt = faceMatchCompletedAt;
  }

  public String getFingerprintTemplateRef() {
    return fingerprintTemplateRef;
  }

  public void setFingerprintTemplateRef(String fingerprintTemplateRef) {
    this.fingerprintTemplateRef = fingerprintTemplateRef;
  }

  public String getFingerprintCapturePath() {
    return fingerprintCapturePath;
  }

  public void setFingerprintCapturePath(String fingerprintCapturePath) {
    this.fingerprintCapturePath = fingerprintCapturePath;
  }

  public BigDecimal getFingerprintQualityScore() {
    return fingerprintQualityScore;
  }

  public void setFingerprintQualityScore(BigDecimal fingerprintQualityScore) {
    this.fingerprintQualityScore = fingerprintQualityScore;
  }

  public String getFingerprintMatchResult() {
    return fingerprintMatchResult;
  }

  public void setFingerprintMatchResult(String fingerprintMatchResult) {
    this.fingerprintMatchResult = fingerprintMatchResult;
  }

  public Instant getFingerprintCompletedAt() {
    return fingerprintCompletedAt;
  }

  public void setFingerprintCompletedAt(Instant fingerprintCompletedAt) {
    this.fingerprintCompletedAt = fingerprintCompletedAt;
  }

  public String getQuizTemplateId() {
    return quizTemplateId;
  }

  public void setQuizTemplateId(String quizTemplateId) {
    this.quizTemplateId = quizTemplateId;
  }

  public Integer getQuizScore() {
    return quizScore;
  }

  public void setQuizScore(Integer quizScore) {
    this.quizScore = quizScore;
  }

  public Integer getQuizMaxScore() {
    return quizMaxScore;
  }

  public void setQuizMaxScore(Integer quizMaxScore) {
    this.quizMaxScore = quizMaxScore;
  }

  public Boolean getQuizPassed() {
    return quizPassed;
  }

  public void setQuizPassed(Boolean quizPassed) {
    this.quizPassed = quizPassed;
  }

  public String getQuizAnswersJson() {
    return quizAnswersJson;
  }

  public void setQuizAnswersJson(String quizAnswersJson) {
    this.quizAnswersJson = quizAnswersJson;
  }

  public Instant getQuizCompletedAt() {
    return quizCompletedAt;
  }

  public void setQuizCompletedAt(Instant quizCompletedAt) {
    this.quizCompletedAt = quizCompletedAt;
  }

  public String getMpinHash() {
    return mpinHash;
  }

  public void setMpinHash(String mpinHash) {
    this.mpinHash = mpinHash;
  }

  public String getFormDataJson() {
    return formDataJson;
  }

  public void setFormDataJson(String formDataJson) {
    this.formDataJson = formDataJson;
  }

  public Instant getFormSubmittedAt() {
    return formSubmittedAt;
  }

  public void setFormSubmittedAt(Instant formSubmittedAt) {
    this.formSubmittedAt = formSubmittedAt;
  }

  public String getAmlCaseReference() {
    return amlCaseReference;
  }

  public void setAmlCaseReference(String amlCaseReference) {
    this.amlCaseReference = amlCaseReference;
  }

  public String getAmlScreeningStatus() {
    return amlScreeningStatus;
  }

  public void setAmlScreeningStatus(String amlScreeningStatus) {
    this.amlScreeningStatus = amlScreeningStatus;
  }

  public String getAmlScreeningSummary() {
    return amlScreeningSummary;
  }

  public void setAmlScreeningSummary(String amlScreeningSummary) {
    this.amlScreeningSummary = amlScreeningSummary;
  }

  public Instant getAmlLastCheckedAt() {
    return amlLastCheckedAt;
  }

  public void setAmlLastCheckedAt(Instant amlLastCheckedAt) {
    this.amlLastCheckedAt = amlLastCheckedAt;
  }

  public String getT24CustomerId() {
    return t24CustomerId;
  }

  public void setT24CustomerId(String t24CustomerId) {
    this.t24CustomerId = t24CustomerId;
  }

  public String getT24AccountId() {
    return t24AccountId;
  }

  public void setT24AccountId(String t24AccountId) {
    this.t24AccountId = t24AccountId;
  }

  public String getT24SubmissionStatus() {
    return t24SubmissionStatus;
  }

  public void setT24SubmissionStatus(String t24SubmissionStatus) {
    this.t24SubmissionStatus = t24SubmissionStatus;
  }

  public String getT24LastError() {
    return t24LastError;
  }

  public void setT24LastError(String t24LastError) {
    this.t24LastError = t24LastError;
  }

  public Instant getT24LastAttemptAt() {
    return t24LastAttemptAt;
  }

  public void setT24LastAttemptAt(Instant t24LastAttemptAt) {
    this.t24LastAttemptAt = t24LastAttemptAt;
  }

  public String getOcrJobId() {
    return ocrJobId;
  }

  public void setOcrJobId(String ocrJobId) {
    this.ocrJobId = ocrJobId;
  }

  public String getOcrStatus() {
    return ocrStatus;
  }

  public void setOcrStatus(String ocrStatus) {
    this.ocrStatus = ocrStatus;
  }

  public String getOcrExtractedJson() {
    return ocrExtractedJson;
  }

  public void setOcrExtractedJson(String ocrExtractedJson) {
    this.ocrExtractedJson = ocrExtractedJson;
  }

  public String getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(String validationErrors) {
    this.validationErrors = validationErrors;
  }

  public Instant getExpireAt() {
    return expireAt;
  }

  public void setExpireAt(Instant expireAt) {
    this.expireAt = expireAt;
  }

  public Instant getInviteSentAt() {
    return inviteSentAt;
  }

  public void setInviteSentAt(Instant inviteSentAt) {
    this.inviteSentAt = inviteSentAt;
  }

  public Integer getInviteResendCount() {
    return inviteResendCount;
  }

  public void setInviteResendCount(Integer inviteResendCount) {
    this.inviteResendCount = inviteResendCount;
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
    if (inviteResendCount == null) {
      inviteResendCount = 0;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
