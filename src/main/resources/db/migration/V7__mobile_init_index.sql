-- index to support mobile-number lookup by status (used in new /auth/init endpoint)
-- MySQL does not support CREATE INDEX IF NOT EXISTS; use idempotent check
SET @idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE()
    AND table_name = 'employee_onboarding'
    AND index_name = 'idx_emp_mobile_status'
);
SET @stmt := IF(@idx = 0,
  'CREATE INDEX idx_emp_mobile_status ON employee_onboarding (mobile, status)',
  'SELECT 1'
);
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;
