CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    telefone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL,
    ultimo_login DATETIME(6),
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_account_email UNIQUE (email)
) ENGINE=InnoDB;

ALTER TABLE teams
    ADD COLUMN responsible_user_id BIGINT NULL,
    ADD CONSTRAINT fk_team_responsible_user
        FOREIGN KEY (responsible_user_id) REFERENCES user_accounts(id);

CREATE INDEX idx_team_responsible_user ON teams(responsible_user_id);

ALTER TABLE competitors
    ADD COLUMN user_account_id BIGINT NULL,
    ADD CONSTRAINT uk_competitor_user_account UNIQUE (user_account_id),
    ADD CONSTRAINT fk_competitor_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts(id);

ALTER TABLE registrations
    ADD COLUMN requested_by_user_id BIGINT NULL,
    ADD COLUMN reviewed_by_user_id BIGINT NULL,
    ADD COLUMN reviewed_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_registration_requested_by
        FOREIGN KEY (requested_by_user_id) REFERENCES user_accounts(id),
    ADD CONSTRAINT fk_registration_reviewed_by
        FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id);

CREATE INDEX idx_registration_requested_by ON registrations(requested_by_user_id);
CREATE INDEX idx_registration_reviewed_by ON registrations(reviewed_by_user_id);

CREATE TABLE registration_competitors (
    registration_id BIGINT NOT NULL,
    competitor_id BIGINT NOT NULL,
    PRIMARY KEY (registration_id, competitor_id),
    CONSTRAINT uk_registration_competitor UNIQUE (registration_id, competitor_id),
    CONSTRAINT fk_registration_competitor_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_registration_competitor_competitor
        FOREIGN KEY (competitor_id) REFERENCES competitors(id)
) ENGINE=InnoDB;

CREATE TABLE robot_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    robot_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    principal BOOLEAN NOT NULL,
    ordem INT NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_robot_image_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_robot_image_robot FOREIGN KEY (robot_id) REFERENCES robots(id)
) ENGINE=InnoDB;

CREATE INDEX idx_robot_image_robot ON robot_images(robot_id);
