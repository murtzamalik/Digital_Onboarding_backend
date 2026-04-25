-- Optional corporate profile fields (captured at client onboarding, shown on mobile review).
ALTER TABLE corporate_clients
  ADD COLUMN trade_name VARCHAR(256) NULL AFTER legal_name,
  ADD COLUMN industry VARCHAR(256) NULL,
  ADD COLUMN registered_address VARCHAR(512) NULL,
  ADD COLUMN city VARCHAR(128) NULL,
  ADD COLUMN contact_phone VARCHAR(64) NULL,
  ADD COLUMN contact_email VARCHAR(320) NULL,
  ADD COLUMN company_registration_no VARCHAR(128) NULL;
