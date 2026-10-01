ALTER TABLE brackets
    ADD COLUMN generation_reason VARCHAR(500) NULL AFTER atual,
    ADD COLUMN generated_by_user_id BIGINT NULL AFTER generation_reason,
    ADD CONSTRAINT fk_bracket_generated_by_user
        FOREIGN KEY (generated_by_user_id) REFERENCES user_accounts(id);

CREATE INDEX idx_bracket_generated_by_user
    ON brackets(generated_by_user_id);
