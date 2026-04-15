package com.bank.cebos.enums;

/**
 * Canonical onboarding journey states (CEBOS v2). Persisted as VARCHAR in the database for
 * Oracle portability (avoid MySQL ENUM types in DDL).
 */
public enum OnboardingStatus {
  UPLOADED,
  INVALID,
  VALIDATED,
  INVITED,
  OTP_PENDING,
  OTP_VERIFIED,
  OCR_IN_PROGRESS,
  OCR_FAILED,
  NADRA_PENDING,
  NADRA_VERIFIED,
  NADRA_FAILED,
  LIVENESS_PENDING,
  LIVENESS_PASSED,
  LIVENESS_FAILED,
  FACE_MATCH_PENDING,
  FACE_MATCHED,
  FACE_MATCH_FAILED,
  FINGERPRINT_PENDING,
  FINGERPRINT_MATCHED,
  FINGERPRINT_FAILED,
  QUIZ_PENDING,
  QUIZ_PASSED,
  QUIZ_FAILED,
  /** Terminal block (incl. quiz-driven blocks per product rules). */
  BLOCKED,
  FORM_PENDING,
  FORM_SUBMITTED,
  AML_CHECK_PENDING,
  AML_REJECTED,
  T24_PENDING,
  T24_FAILED,
  ACCOUNT_OPENED,
  EXPIRED,
}
