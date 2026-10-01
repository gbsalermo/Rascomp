ALTER TABLE match_results
    ADD COLUMN correction_reason VARCHAR(500) NULL AFTER observacao,
    ADD COLUMN corrected_by_user_id BIGINT NULL AFTER correction_reason,
    ADD COLUMN corrected_at DATETIME(6) NULL AFTER corrected_by_user_id,
    ADD CONSTRAINT fk_match_result_corrected_by
        FOREIGN KEY (corrected_by_user_id) REFERENCES user_accounts(id);

CREATE INDEX idx_match_result_corrected_by
    ON match_results(corrected_by_user_id);
