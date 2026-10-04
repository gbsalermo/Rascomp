CREATE TABLE follow_take_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    tomada INT NOT NULL,
    data_hora DATETIME(6) NOT NULL,
    pista VARCHAR(80) NULL,
    ordem_execucao INT NULL,
    status VARCHAR(24) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_take_schedule_context
        UNIQUE (competition_id, category_id, tomada),
    CONSTRAINT fk_follow_take_schedule_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_follow_take_schedule_category
        FOREIGN KEY (category_id) REFERENCES competition_categories(id)
);

CREATE INDEX idx_follow_take_schedule_competition
    ON follow_take_schedules (competition_id, data_hora, ordem_execucao);

CREATE TABLE follow_take_schedule_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    ordem_convocacao INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_take_schedule_entry
        UNIQUE (schedule_id, registration_id),
    CONSTRAINT fk_follow_take_schedule_entry_schedule
        FOREIGN KEY (schedule_id) REFERENCES follow_take_schedules(id),
    CONSTRAINT fk_follow_take_schedule_entry_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id)
);

CREATE INDEX idx_follow_take_schedule_entry_order
    ON follow_take_schedule_entries (schedule_id, ordem_convocacao);
