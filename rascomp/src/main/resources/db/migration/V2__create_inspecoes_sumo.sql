CREATE TABLE inspecoes_sumo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    numero_tentativa INT NOT NULL,
    peso_medido DECIMAL(8,3) NOT NULL,
    aprovada BOOLEAN NOT NULL,
    observacao VARCHAR(500),
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_inspecao_sumo_registration_tentativa
        UNIQUE (registration_id, numero_tentativa),
    CONSTRAINT fk_inspecao_sumo_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id)
);

CREATE INDEX idx_inspecao_sumo_registration
    ON inspecoes_sumo (registration_id);
