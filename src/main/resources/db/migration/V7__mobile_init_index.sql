-- index to support mobile-number lookup by status (used in new /auth/init endpoint)
CREATE INDEX IF NOT EXISTS idx_emp_mobile_status
    ON employee_onboarding(mobile, status);
