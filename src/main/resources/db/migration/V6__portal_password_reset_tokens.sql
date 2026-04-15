-- Corporate portal password reset: store SHA-256 hex digest of opaque token only (never raw token).
-- One row per issued reset link; validate unused (used_at IS NULL) and unexpired.

CREATE TABLE portal_password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  corporate_user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  used_at DATETIME(3) NULL,
  request_ip VARCHAR(64) NULL,
  correlation_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_portal_password_reset_token_hash (token_hash),
  KEY idx_portal_password_reset_user (corporate_user_id),
  KEY idx_portal_password_reset_expires (expires_at),
  CONSTRAINT fk_portal_password_reset_user FOREIGN KEY (corporate_user_id) REFERENCES corporate_users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
