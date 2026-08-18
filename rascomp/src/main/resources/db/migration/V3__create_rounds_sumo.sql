CREATE TABLE rounds_sumo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    numero_round INT NOT NULL,
    winner_registration_id BIGINT,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(500),
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_round_sumo_match_numero
        UNIQUE (match_id, numero_round),
    CONSTRAINT fk_round_sumo_match
        FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_round_sumo_winner_registration
        FOREIGN KEY (winner_registration_id) REFERENCES registrations(id)
);

CREATE INDEX idx_round_sumo_match
    ON rounds_sumo (match_id);
