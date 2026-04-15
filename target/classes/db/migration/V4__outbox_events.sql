-- Transactional outbox for async side effects (same-DB commit as domain writes).
-- Timestamps: DATETIME(3), UTC via connectionTimeZone=UTC.

CREATE TABLE outbox_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_type VARCHAR(128) NOT NULL,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  processed_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_outbox_events_processed_created (processed_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
