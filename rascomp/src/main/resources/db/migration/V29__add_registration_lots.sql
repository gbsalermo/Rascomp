CREATE TABLE registration_lots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_registration_lot_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT uk_registration_lot_competition_name
        UNIQUE (competition_id, nome)
);

CREATE INDEX idx_registration_lot_competition_dates
    ON registration_lots (competition_id, ativo, data_inicio, data_fim);

ALTER TABLE registrations
    ADD COLUMN registration_lot_id BIGINT NULL,
    ADD CONSTRAINT fk_registration_lot_robot_registration
        FOREIGN KEY (registration_lot_id) REFERENCES registration_lots(id);

ALTER TABLE participant_competition_registrations
    ADD COLUMN registration_lot_id BIGINT NULL,
    ADD CONSTRAINT fk_registration_lot_participant_registration
        FOREIGN KEY (registration_lot_id) REFERENCES registration_lots(id);
