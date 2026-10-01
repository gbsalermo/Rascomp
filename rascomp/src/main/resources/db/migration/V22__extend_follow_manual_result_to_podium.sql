ALTER TABLE follow_manual_results
    ADD COLUMN second_registration_id BIGINT NULL AFTER winner_registration_id,
    ADD COLUMN third_registration_id BIGINT NULL AFTER second_registration_id,
    ADD CONSTRAINT fk_follow_manual_result_second
        FOREIGN KEY (second_registration_id) REFERENCES registrations(id),
    ADD CONSTRAINT fk_follow_manual_result_third
        FOREIGN KEY (third_registration_id) REFERENCES registrations(id);

CREATE INDEX idx_follow_manual_result_second
    ON follow_manual_results(second_registration_id);

CREATE INDEX idx_follow_manual_result_third
    ON follow_manual_results(third_registration_id);
