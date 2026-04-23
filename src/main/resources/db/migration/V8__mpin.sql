ALTER TABLE employee_onboarding
    ADD COLUMN mpin_hash VARCHAR(256) NULL COMMENT 'BCrypt hash of employee MPIN';
