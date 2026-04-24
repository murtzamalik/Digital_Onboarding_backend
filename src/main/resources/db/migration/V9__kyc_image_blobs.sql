-- KYC images persisted in DB (raw JPEG/PNG bytes). Path columns may remain for legacy rows.
ALTER TABLE employee_onboarding
  ADD COLUMN cnic_front_image_data LONGBLOB NULL,
  ADD COLUMN cnic_back_image_data LONGBLOB NULL,
  ADD COLUMN selfie_image_data LONGBLOB NULL;
