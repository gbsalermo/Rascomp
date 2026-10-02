ALTER TABLE robots
    ADD COLUMN created_by_user_id BIGINT NULL,
    ADD CONSTRAINT fk_robot_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts(id);

UPDATE robots r
JOIN (
    SELECT robot_id, MIN(created_by_user_id) AS created_by_user_id
    FROM robot_responsibles
    WHERE created_by_user_id IS NOT NULL
    GROUP BY robot_id
) rr ON rr.robot_id = r.id
SET r.created_by_user_id = rr.created_by_user_id
WHERE r.created_by_user_id IS NULL;

CREATE INDEX idx_robot_created_by
    ON robots(created_by_user_id);

CREATE TABLE registration_competitor_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    competitor_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    actor_user_id BIGINT NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    reason VARCHAR(500) NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_registration_comp_change_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_registration_comp_change_competitor
        FOREIGN KEY (competitor_id) REFERENCES competitors(id),
    CONSTRAINT fk_registration_comp_change_actor
        FOREIGN KEY (actor_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_registration_comp_change_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_registration_comp_change_registration
    ON registration_competitor_changes(registration_id, status);

CREATE INDEX idx_registration_comp_change_competitor
    ON registration_competitor_changes(competitor_id, status);

CREATE TABLE participant_registration_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participant_registration_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_participant_reg_history_registration
        FOREIGN KEY (participant_registration_id)
        REFERENCES participant_competition_registrations(id),
    CONSTRAINT fk_participant_reg_history_actor
        FOREIGN KEY (actor_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_participant_reg_history_registration
    ON participant_registration_status_history(participant_registration_id, data_cadastro);

CREATE TABLE team_leadership_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    competition_id BIGINT NULL,
    previous_user_id BIGINT NULL,
    new_user_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    data_cadastro DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_team_leadership_history_team
        FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_leadership_history_competition
        FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_team_leadership_history_previous
        FOREIGN KEY (previous_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_team_leadership_history_new
        FOREIGN KEY (new_user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_team_leadership_history_actor
        FOREIGN KEY (changed_by_user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_team_leadership_history_team
    ON team_leadership_history(team_id, data_cadastro);
