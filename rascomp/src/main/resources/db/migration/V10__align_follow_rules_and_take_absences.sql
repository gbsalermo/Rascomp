UPDATE config_follow
SET numero_tomadas = 3,
    tentativas_por_tomada = 3;

ALTER TABLE config_follow
    ADD COLUMN penalidade_padrao_segundos INT NOT NULL DEFAULT 10 AFTER numero_checkpoints,
    ADD COLUMN tempo_apresentacao_segundos INT NOT NULL DEFAULT 60 AFTER penalidade_padrao_segundos;

CREATE TABLE ausencias_tomada_seguidor_linha (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    tomada INT NOT NULL,
    observacao VARCHAR(500) NULL,
    registrado_por_user_id BIGINT NOT NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_ausencia_follow_registration_tomada UNIQUE (registration_id, tomada),
    CONSTRAINT fk_ausencia_follow_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_ausencia_follow_registrado_por
        FOREIGN KEY (registrado_por_user_id) REFERENCES user_accounts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_ausencia_follow_registration
    ON ausencias_tomada_seguidor_linha (registration_id);
