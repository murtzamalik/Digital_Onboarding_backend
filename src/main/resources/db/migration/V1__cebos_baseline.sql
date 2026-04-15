-- CEBOS baseline schema (v1 core + mock + v2 admin/audit/config/quota/corrections).
-- Timestamps: DATETIME(3), interpreted as UTC by the application (connectionTimeZone=UTC).
-- Charset: utf8mb4 for full Unicode.
-- Status / domain picklists: VARCHAR (not MySQL ENUM) for Oracle portability.

CREATE TABLE corporate_clients (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  client_code VARCHAR(64) NOT NULL,
  legal_name VARCHAR(512) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_corporate_clients_public_id (public_id),
  UNIQUE KEY uk_corporate_clients_client_code (client_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE corporate_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_client_id BIGINT NOT NULL,
  email VARCHAR(320) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(512) NOT NULL,
  role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_corporate_users_email_client (corporate_client_id, email),
  KEY idx_corporate_users_client (corporate_client_id),
  CONSTRAINT fk_corporate_users_client FOREIGN KEY (corporate_client_id) REFERENCES corporate_clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE upload_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_client_id BIGINT NOT NULL,
  uploaded_by_user_id BIGINT NOT NULL,
  batch_reference VARCHAR(64) NOT NULL,
  original_filename VARCHAR(512) NOT NULL,
  storage_path VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL,
  total_rows INT NOT NULL DEFAULT 0,
  valid_row_count INT NOT NULL DEFAULT 0,
  invalid_row_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_upload_batches_reference (batch_reference),
  KEY idx_upload_batches_client (corporate_client_id),
  KEY idx_upload_batches_status (status),
  CONSTRAINT fk_upload_batches_client FOREIGN KEY (corporate_client_id) REFERENCES corporate_clients (id),
  CONSTRAINT fk_upload_batches_user FOREIGN KEY (uploaded_by_user_id) REFERENCES corporate_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE correction_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_client_id BIGINT NOT NULL,
  source_batch_id BIGINT NOT NULL,
  correction_reference VARCHAR(64) NOT NULL,
  original_filename VARCHAR(512) NULL,
  storage_path VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_correction_batches_reference (correction_reference),
  KEY idx_correction_batches_client (corporate_client_id),
  KEY idx_correction_batches_source (source_batch_id),
  CONSTRAINT fk_correction_batches_client FOREIGN KEY (corporate_client_id) REFERENCES corporate_clients (id),
  CONSTRAINT fk_correction_batches_upload FOREIGN KEY (source_batch_id) REFERENCES upload_batches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_onboarding (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_ref VARCHAR(64) NOT NULL,
  batch_id BIGINT NOT NULL,
  correction_batch_id BIGINT NULL,
  corporate_client_id BIGINT NOT NULL,
  -- Journey status: VARCHAR aligns with com.bank.cebos.enums.OnboardingStatus (max length 64).
  status VARCHAR(64) NOT NULL,
  block_reason VARCHAR(512) NULL,
  blocked_at DATETIME(3) NULL,
  unblocked_by VARCHAR(256) NULL,
  unblocked_at DATETIME(3) NULL,
  cnic VARCHAR(32) NULL,
  mobile VARCHAR(32) NULL,
  email VARCHAR(320) NULL,
  full_name VARCHAR(512) NULL,
  father_name VARCHAR(512) NULL,
  mother_name VARCHAR(512) NULL,
  date_of_birth DATE NULL,
  gender VARCHAR(32) NULL,
  religion VARCHAR(128) NULL,
  cnic_issue_date DATE NULL,
  cnic_expiry_date DATE NULL,
  present_address_line1 VARCHAR(512) NULL,
  present_address_line2 VARCHAR(512) NULL,
  present_city VARCHAR(128) NULL,
  present_country VARCHAR(128) NULL,
  permanent_address_line1 VARCHAR(512) NULL,
  permanent_address_line2 VARCHAR(512) NULL,
  permanent_city VARCHAR(128) NULL,
  permanent_country VARCHAR(128) NULL,
  nadra_transaction_id VARCHAR(128) NULL,
  nadra_verification_status VARCHAR(64) NULL,
  nadra_verification_code VARCHAR(64) NULL,
  nadra_response_payload TEXT NULL,
  nadra_verified_at DATETIME(3) NULL,
  cnic_front_image_path VARCHAR(1024) NULL,
  cnic_back_image_path VARCHAR(1024) NULL,
  selfie_image_path VARCHAR(1024) NULL,
  liveness_session_id VARCHAR(128) NULL,
  liveness_vendor_ref VARCHAR(256) NULL,
  liveness_score DECIMAL(7,4) NULL,
  liveness_result VARCHAR(32) NULL,
  liveness_completed_at DATETIME(3) NULL,
  face_match_score DECIMAL(7,4) NULL,
  face_match_result VARCHAR(32) NULL,
  face_match_completed_at DATETIME(3) NULL,
  fingerprint_template_ref VARCHAR(256) NULL,
  fingerprint_capture_path VARCHAR(1024) NULL,
  fingerprint_quality_score DECIMAL(7,4) NULL,
  fingerprint_match_result VARCHAR(32) NULL,
  fingerprint_completed_at DATETIME(3) NULL,
  quiz_template_id VARCHAR(64) NULL,
  quiz_score INT NULL,
  quiz_max_score INT NULL,
  quiz_passed TINYINT(1) NULL,
  quiz_answers_json TEXT NULL,
  quiz_completed_at DATETIME(3) NULL,
  form_data_json TEXT NULL,
  form_submitted_at DATETIME(3) NULL,
  aml_case_reference VARCHAR(128) NULL,
  aml_screening_status VARCHAR(64) NULL,
  aml_screening_summary TEXT NULL,
  aml_last_checked_at DATETIME(3) NULL,
  t24_customer_id VARCHAR(64) NULL,
  t24_account_id VARCHAR(64) NULL,
  t24_submission_status VARCHAR(64) NULL,
  t24_last_error TEXT NULL,
  t24_last_attempt_at DATETIME(3) NULL,
  ocr_job_id VARCHAR(128) NULL,
  ocr_status VARCHAR(64) NULL,
  ocr_extracted_json TEXT NULL,
  validation_errors TEXT NULL,
  expire_at DATETIME(3) NULL,
  invite_sent_at DATETIME(3) NULL,
  invite_resend_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_employee_onboarding_ref (employee_ref),
  KEY idx_employee_onboarding_status_updated (status, updated_at),
  KEY idx_employee_onboarding_batch_status (batch_id, status),
  KEY idx_employee_onboarding_client (corporate_client_id),
  KEY idx_employee_onboarding_correction_batch (correction_batch_id),
  CONSTRAINT fk_employee_onboarding_client FOREIGN KEY (corporate_client_id) REFERENCES corporate_clients (id),
  CONSTRAINT fk_employee_onboarding_batch FOREIGN KEY (batch_id) REFERENCES upload_batches (id),
  CONSTRAINT fk_employee_onboarding_correction FOREIGN KEY (correction_batch_id) REFERENCES correction_batches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_onboarding_id BIGINT NOT NULL,
  from_status VARCHAR(64) NULL,
  to_status VARCHAR(64) NOT NULL,
  changed_by VARCHAR(256) NOT NULL,
  reason VARCHAR(1024) NULL,
  ip_address VARCHAR(64) NULL,
  correlation_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_employee_status_history_employee (employee_onboarding_id),
  CONSTRAINT fk_employee_status_history_employee FOREIGN KEY (employee_onboarding_id) REFERENCES employee_onboarding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE otp_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_onboarding_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  destination_masked VARCHAR(64) NULL,
  otp_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 5,
  verified_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_otp_sessions_employee (employee_onboarding_id),
  KEY idx_otp_sessions_expires (expires_at),
  CONSTRAINT fk_otp_sessions_employee FOREIGN KEY (employee_onboarding_id) REFERENCES employee_onboarding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE biometric_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_onboarding_id BIGINT NOT NULL,
  biometric_type VARCHAR(32) NOT NULL,
  vendor VARCHAR(64) NULL,
  result_status VARCHAR(32) NOT NULL,
  score DECIMAL(7,4) NULL,
  request_payload TEXT NULL,
  response_payload TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_biometric_logs_employee (employee_onboarding_id),
  CONSTRAINT fk_biometric_logs_employee FOREIGN KEY (employee_onboarding_id) REFERENCES employee_onboarding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_attempts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_onboarding_id BIGINT NOT NULL,
  attempt_number INT NOT NULL,
  score INT NULL,
  max_score INT NULL,
  passed TINYINT(1) NULL,
  answers_json TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_quiz_attempts_employee (employee_onboarding_id),
  CONSTRAINT fk_quiz_attempts_employee FOREIGN KEY (employee_onboarding_id) REFERENCES employee_onboarding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_nadra_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cnic VARCHAR(32) NOT NULL,
  full_name VARCHAR(512) NULL,
  father_name VARCHAR(512) NULL,
  mother_name VARCHAR(512) NULL,
  date_of_birth DATE NULL,
  present_address TEXT NULL,
  record_payload TEXT NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_mock_nadra_cnic (cnic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_t24_customers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  external_customer_id VARCHAR(64) NOT NULL,
  cnic VARCHAR(32) NULL,
  full_name VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL,
  payload TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_mock_t24_customer_ext (external_customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_t24_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  mock_t24_customer_id BIGINT NOT NULL,
  account_number VARCHAR(64) NOT NULL,
  currency VARCHAR(8) NOT NULL,
  status VARCHAR(32) NOT NULL,
  payload TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_mock_t24_accounts_customer (mock_t24_customer_id),
  CONSTRAINT fk_mock_t24_accounts_customer FOREIGN KEY (mock_t24_customer_id) REFERENCES mock_t24_customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_sms_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  to_mobile VARCHAR(32) NOT NULL,
  message_body VARCHAR(2000) NOT NULL,
  provider_ref VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_mock_sms_log_mobile (to_mobile),
  KEY idx_mock_sms_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_aml_blacklist (
  id BIGINT NOT NULL AUTO_INCREMENT,
  match_type VARCHAR(32) NOT NULL,
  cnic VARCHAR(32) NULL,
  name_pattern VARCHAR(512) NULL,
  risk_level VARCHAR(32) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_mock_aml_blacklist_cnic (cnic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_mother_names (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cnic VARCHAR(32) NOT NULL,
  mother_name VARCHAR(512) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_mock_mother_names_cnic (cnic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bank_admin_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(320) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(512) NOT NULL,
  role VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(3) NULL,
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_admin_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE portal_user_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_user_id BIGINT NULL,
  action VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NULL,
  resource_id VARCHAR(64) NULL,
  detail_json TEXT NULL,
  ip_address VARCHAR(64) NULL,
  correlation_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_portal_audit_user (corporate_user_id),
  KEY idx_portal_audit_created (created_at),
  CONSTRAINT fk_portal_audit_user FOREIGN KEY (corporate_user_id) REFERENCES corporate_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_notification_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_email VARCHAR(320) NOT NULL,
  template_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  payload_json TEXT NULL,
  error_message VARCHAR(2000) NULL,
  sent_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_email_notification_status (status),
  KEY idx_email_notification_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_configuration (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_key VARCHAR(256) NOT NULL,
  config_value TEXT NULL,
  value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
  description VARCHAR(512) NULL,
  updated_by VARCHAR(256) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_configuration_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE corporate_client_quota_monthly (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_client_id BIGINT NOT NULL,
  `year_month` CHAR(7) NOT NULL,
  quota_limit INT NOT NULL,
  used_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_quota_client_month (corporate_client_id, `year_month`),
  CONSTRAINT fk_quota_client FOREIGN KEY (corporate_client_id) REFERENCES corporate_clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
