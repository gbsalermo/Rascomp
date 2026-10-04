CREATE TABLE registration_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    actor_user_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_registration_status_history_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_registration_status_history_actor
        FOREIGN KEY (actor_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_registration_status_history_registration
    ON registration_status_history (registration_id, data_cadastro);
