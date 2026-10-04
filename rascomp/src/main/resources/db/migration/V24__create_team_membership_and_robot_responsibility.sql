CREATE TABLE team_membership_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    participant_user_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mensagem VARCHAR(500) NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_team_membership_team
        FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_membership_participant
        FOREIGN KEY (participant_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_team_membership_requested_by
        FOREIGN KEY (requested_by_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_team_membership_reviewed_by
        FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_team_membership_team_status
    ON team_membership_requests(team_id, status);

CREATE INDEX idx_team_membership_participant_status
    ON team_membership_requests(participant_user_id, status);

CREATE TABLE robot_responsibles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    robot_id BIGINT NOT NULL,
    competitor_id BIGINT NOT NULL,
    created_by_user_id BIGINT NULL,
    ativo BIT(1) NOT NULL DEFAULT b'1',
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_robot_responsible UNIQUE (robot_id, competitor_id),
    CONSTRAINT fk_robot_responsible_robot
        FOREIGN KEY (robot_id) REFERENCES robots(id),
    CONSTRAINT fk_robot_responsible_competitor
        FOREIGN KEY (competitor_id) REFERENCES competitors(id),
    CONSTRAINT fk_robot_responsible_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_robot_responsible_competitor
    ON robot_responsibles(competitor_id, ativo);

CREATE INDEX idx_robot_responsible_robot
    ON robot_responsibles(robot_id, ativo);
