ALTER TABLE competition_categories
    ADD COLUMN sumo_control_mode VARCHAR(20) NULL AFTER sumo_physical_class;

UPDATE competition_categories
SET sumo_control_mode = CASE
    WHEN LOWER(nome) LIKE '%auto%' THEN 'AUTONOMO'
    ELSE 'RC'
END
WHERE modalidade = 'SUMO' AND sumo_control_mode IS NULL;

ALTER TABLE config_sumo
    ADD COLUMN max_rounds_extras INT NOT NULL DEFAULT 2 AFTER permite_round_desempate;

UPDATE config_sumo
SET max_rounds_extras = CASE
    WHEN permite_round_desempate = TRUE THEN 2
    ELSE 0
END;

ALTER TABLE inspecoes_sumo
    MODIFY COLUMN peso_medido DECIMAL(8,3) NULL,
    ADD COLUMN registrado_por_user_id BIGINT NULL AFTER observacao,
    ADD CONSTRAINT fk_inspecao_sumo_registrado_por
        FOREIGN KEY (registrado_por_user_id) REFERENCES user_accounts(id);

CREATE INDEX idx_inspecao_sumo_registrado_por
    ON inspecoes_sumo (registrado_por_user_id);

ALTER TABLE rounds_sumo
    ADD COLUMN justificativa VARCHAR(500) NULL AFTER observacao;

CREATE TABLE competition_judges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    user_account_id BIGINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_competition_judge_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_competition_judge_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_competition_judge_competition
    ON competition_judges (competition_id, ativo);

CREATE TABLE match_judge_decisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    winner_registration_id BIGINT NOT NULL,
    judge_id BIGINT NOT NULL,
    justificativa VARCHAR(500) NOT NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_match_judge_decision_match UNIQUE (match_id),
    CONSTRAINT fk_match_judge_decision_match
        FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_judge_decision_winner
        FOREIGN KEY (winner_registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_match_judge_decision_judge
        FOREIGN KEY (judge_id) REFERENCES competition_judges(id)
) ENGINE=InnoDB;

CREATE INDEX idx_match_judge_decision_judge
    ON match_judge_decisions (judge_id);
