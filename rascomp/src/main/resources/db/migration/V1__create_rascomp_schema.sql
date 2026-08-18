CREATE TABLE competition_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    modalidade VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE config_sumo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_category_id BIGINT NOT NULL,
    peso_max DECIMAL(8,3) NOT NULL,
    exige_inspecao BOOLEAN NOT NULL,
    max_tentativas_inspecao INT NOT NULL,
    numero_rounds INT NOT NULL,
    rounds_para_vencer INT NOT NULL,
    permite_round_desempate BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_config_sumo_category UNIQUE (competition_category_id),
    CONSTRAINT fk_config_sumo_category FOREIGN KEY (competition_category_id) REFERENCES competition_categories(id)
) ENGINE=InnoDB;

CREATE TABLE config_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_category_id BIGINT NOT NULL,
    numero_tomadas INT NOT NULL,
    tentativas_por_tomada INT NOT NULL,
    max_tempo_segundos INT NOT NULL,
    numero_checkpoints INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_config_follow_category UNIQUE (competition_category_id),
    CONSTRAINT fk_config_follow_category FOREIGN KEY (competition_category_id) REFERENCES competition_categories(id)
) ENGINE=InnoDB;

CREATE TABLE institutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    sigla VARCHAR(20) NOT NULL,
    cidade VARCHAR(100),
    estado VARCHAR(2),
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_institution_sigla UNIQUE (sigla)
) ENGINE=InnoDB;

CREATE TABLE teams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    institution_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_team_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
) ENGINE=InnoDB;

CREATE TABLE competitors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefone VARCHAR(20),
    team_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_competitor_email UNIQUE (email),
    CONSTRAINT fk_competitor_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB;

CREATE TABLE robots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    team_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_robot_nome_team UNIQUE (nome, team_id),
    CONSTRAINT fk_robot_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB;

CREATE TABLE competitions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    inicio_inscricoes DATE NOT NULL,
    fim_inscricoes DATE NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    robot_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(500),
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_registration_competition_category_robot UNIQUE (competition_id, category_id, robot_id),
    CONSTRAINT fk_registration_competition FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_registration_category FOREIGN KEY (category_id) REFERENCES competition_categories(id),
    CONSTRAINT fk_registration_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_registration_robot FOREIGN KEY (robot_id) REFERENCES robots(id)
) ENGINE=InnoDB;

CREATE TABLE tentativas_seguidor_linha (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    tomada INT NOT NULL,
    numero_tentativa INT NOT NULL,
    tempo_segundos DECIMAL(10,3),
    checkpoints_alcancados INT NOT NULL,
    penalidade_segundos INT NOT NULL,
    concluida BOOLEAN NOT NULL,
    valida BOOLEAN NOT NULL,
    observacao VARCHAR(500),
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tentativa_registration_tomada_numero UNIQUE (registration_id, tomada, numero_tentativa),
    CONSTRAINT fk_tentativa_registration FOREIGN KEY (registration_id) REFERENCES registrations(id)
) ENGINE=InnoDB;

CREATE TABLE brackets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bracket_competition_category UNIQUE (competition_id, category_id),
    CONSTRAINT fk_bracket_competition FOREIGN KEY (competition_id) REFERENCES competitions(id),
    CONSTRAINT fk_bracket_category FOREIGN KEY (category_id) REFERENCES competition_categories(id)
) ENGINE=InnoDB;

CREATE TABLE matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bracket_id BIGINT NOT NULL,
    rodada INT NOT NULL,
    ordem INT NOT NULL,
    registration_a_id BIGINT,
    registration_b_id BIGINT,
    data_hora DATETIME(6),
    status VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_match_bracket_rodada_ordem UNIQUE (bracket_id, rodada, ordem),
    CONSTRAINT fk_match_bracket FOREIGN KEY (bracket_id) REFERENCES brackets(id),
    CONSTRAINT fk_match_registration_a FOREIGN KEY (registration_a_id) REFERENCES registrations(id),
    CONSTRAINT fk_match_registration_b FOREIGN KEY (registration_b_id) REFERENCES registrations(id)
) ENGINE=InnoDB;

CREATE TABLE match_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    winner_registration_id BIGINT,
    pontos_a INT NOT NULL,
    pontos_b INT NOT NULL,
    observacao VARCHAR(500),
    data_cadastro DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_match_result_match UNIQUE (match_id),
    CONSTRAINT fk_match_result_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_result_winner FOREIGN KEY (winner_registration_id) REFERENCES registrations(id)
) ENGINE=InnoDB;
