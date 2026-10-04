CREATE TABLE follow_manual_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    winner_registration_id BIGINT NOT NULL,
    decided_by_user_id BIGINT NOT NULL,
    justificativa VARCHAR(500) NOT NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_manual_result_context UNIQUE (competition_id, category_id),
    CONSTRAINT fk_follow_manual_result_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_follow_manual_result_category
        FOREIGN KEY (category_id) REFERENCES competition_categories(id),
    CONSTRAINT fk_follow_manual_result_winner
        FOREIGN KEY (winner_registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_follow_manual_result_decided_by
        FOREIGN KEY (decided_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_follow_manual_result_context
    ON follow_manual_results (competition_id, category_id);
