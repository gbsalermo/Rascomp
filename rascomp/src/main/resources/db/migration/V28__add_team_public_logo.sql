ALTER TABLE teams
    ADD COLUMN logo_storage_key VARCHAR(500) NULL,
    ADD COLUMN logo_original_filename VARCHAR(255) NULL,
    ADD COLUMN logo_content_type VARCHAR(100) NULL;
