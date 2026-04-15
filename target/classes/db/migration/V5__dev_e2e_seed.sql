-- Dev E2E seed (local / fresh DB). Password for portal@e2e.local is E2E-dev-password!
-- Mobile employee ref for dev login: E2E-EMP-001 (status INVITED).

INSERT INTO corporate_clients (public_id, client_code, legal_name, status)
VALUES (
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
    'E2E-CLIENT',
    'E2E Demo Client',
    'ACTIVE'
);

SET @e2e_client_id = LAST_INSERT_ID();

INSERT INTO corporate_users (
    corporate_client_id,
    email,
    password_hash,
    full_name,
    role,
    status
)
VALUES (
    @e2e_client_id,
    'portal@e2e.local',
    '$2b$12$hBgoKlliOjmCD6RIzgs/uOap4u/0DdMXw1g3ENwdDk4IycxDsceyC',
    'E2E Portal Admin',
    'ADMIN',
    'ACTIVE'
);

SET @e2e_user_id = LAST_INSERT_ID();

INSERT INTO upload_batches (
    corporate_client_id,
    uploaded_by_user_id,
    batch_reference,
    original_filename,
    storage_path,
    status,
    total_rows,
    valid_row_count,
    invalid_row_count
)
VALUES (
    @e2e_client_id,
    @e2e_user_id,
    'E2E-BATCH-001',
    'e2e-seed.xlsx',
    NULL,
    'READY',
    1,
    1,
    0
);

SET @e2e_batch_id = LAST_INSERT_ID();

INSERT INTO employee_onboarding (
    employee_ref,
    batch_id,
    corporate_client_id,
    status,
    mobile,
    full_name,
    expire_at,
    invite_sent_at
)
VALUES (
    'E2E-EMP-001',
    @e2e_batch_id,
    @e2e_client_id,
    'INVITED',
    '+923001234567',
    'E2E Employee',
    DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 30 DAY),
    UTC_TIMESTAMP(3)
);

INSERT INTO system_configuration (config_key, config_value, value_type, description, updated_by, updated_at)
VALUES
  (
    'mobile.min_supported_version',
    '1.0.0',
    'STRING',
    'Minimum mobile app version allowed to proceed',
    'seed:dev-e2e',
    UTC_TIMESTAMP(3)
  ),
  (
    'mobile.force_update_enabled',
    'false',
    'BOOLEAN',
    'When true, mobile clients must upgrade before continuing',
    'seed:dev-e2e',
    UTC_TIMESTAMP(3)
  ),
  (
    'mobile.quiz.template_json',
    '{"templateId":"CEBOS-QZ-1","passingScorePercent":67,"questions":[{"questionId":"Q1","prompt":"What is your onboarding employee reference?","options":["Stored on invite","Always your CNIC","Always your mobile"]},{"questionId":"Q2","prompt":"Which channel issues your invite?","options":["Corporate onboarding","ATM machine","Physical branch only"]},{"questionId":"Q3","prompt":"When should OTP be shared?","options":["Never","With any caller","On social media"]}]}',
    'JSON',
    'Config-driven mobile quiz template',
    'seed:dev-e2e',
    UTC_TIMESTAMP(3)
  ),
  (
    'mobile.quiz.answer_key_json',
    '{"Q1":"INVITE","Q2":"CORPORATE","Q3":"NEVER"}',
    'JSON',
    'Config-driven keyword answer key for mobile quiz scoring',
    'seed:dev-e2e',
    UTC_TIMESTAMP(3)
  ),
  (
    'mobile.form.schema_json',
    '{"templateId":"CEBOS-FORM-1","fields":[{"key":"fullName","label":"Full Name","inputType":"text","required":true},{"key":"email","label":"Email","inputType":"email","required":true},{"key":"motherName","label":"Mother Name","inputType":"text","required":true},{"key":"presentAddress","label":"Present Address","inputType":"text","required":true},{"key":"permanentAddress","label":"Permanent Address","inputType":"text","required":true}]}',
    'JSON',
    'Config-driven mobile dynamic form schema',
    'seed:dev-e2e',
    UTC_TIMESTAMP(3)
  )
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  value_type = VALUES(value_type),
  description = VALUES(description),
  updated_by = VALUES(updated_by),
  updated_at = VALUES(updated_at);
