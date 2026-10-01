CREATE TABLE participant_competition_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    competitor_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    review_reason VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(500) NULL,
    payment_receipt_storage_key VARCHAR(500) NULL,
    payment_receipt_original_name VARCHAR(255) NULL,
    payment_receipt_content_type VARCHAR(100) NULL,
    ativo BIT(1) NOT NULL DEFAULT b'1',
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_participant_competition_registration
        UNIQUE (competition_id, competitor_id),
    CONSTRAINT fk_participant_registration_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_participant_registration_competitor
        FOREIGN KEY (competitor_id) REFERENCES competitors(id),
    CONSTRAINT fk_participant_registration_requested_by
        FOREIGN KEY (requested_by_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_participant_registration_reviewed_by
        FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_participant_registration_competition_status
    ON participant_competition_registrations(competition_id, status);

CREATE INDEX idx_participant_registration_competitor_status
    ON participant_competition_registrations(competitor_id, status);

ALTER TABLE registrations
    ADD COLUMN payment_receipt_storage_key VARCHAR(500) NULL,
    ADD COLUMN payment_receipt_original_name VARCHAR(255) NULL,
    ADD COLUMN payment_receipt_content_type VARCHAR(100) NULL;
