CREATE TABLE registration_cancellation_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    reviewed_by_user_id BIGINT,
    reviewed_at DATETIME(6),
    resposta VARCHAR(500),
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_cancel_request_registration_status (registration_id, status),
    INDEX idx_cancel_request_competition_lookup (registration_id, data_cadastro),
    CONSTRAINT fk_cancel_request_registration FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_cancel_request_requested_user FOREIGN KEY (requested_by_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_cancel_request_reviewed_user FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE competition_registration_window_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    previous_end_date DATE NOT NULL,
    new_end_date DATE NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    performed_by_user_id BIGINT NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_registration_window_competition_date (competition_id, data_cadastro),
    CONSTRAINT fk_registration_window_competition FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_registration_window_user FOREIGN KEY (performed_by_user_id) REFERENCES user_accounts(id)
) ENGINE=InnoDB;
