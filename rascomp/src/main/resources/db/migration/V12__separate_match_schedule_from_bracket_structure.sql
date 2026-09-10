ALTER TABLE matches
    ADD COLUMN pista VARCHAR(80) NULL AFTER data_hora,
    ADD COLUMN ordem_execucao INT NULL AFTER pista,
    ADD COLUMN status_convocacao VARCHAR(24) NOT NULL DEFAULT 'NAO_CONVOCADA' AFTER ordem_execucao;
