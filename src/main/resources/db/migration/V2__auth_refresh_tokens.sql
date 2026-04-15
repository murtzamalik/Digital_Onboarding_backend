-- Opaque refresh tokens: store SHA-256 hex digest only (never the raw token).

CREATE TABLE auth_refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token_hash VARCHAR(64) NOT NULL,
  principal_kind VARCHAR(16) NOT NULL,
  principal_id BIGINT NOT NULL,
  corporate_client_id BIGINT NULL,
  expires_at DATETIME(3) NOT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_refresh_tokens_hash (token_hash),
  KEY idx_auth_refresh_tokens_principal (principal_kind, principal_id),
  KEY idx_auth_refresh_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
