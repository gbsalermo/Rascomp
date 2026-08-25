ALTER TABLE brackets
    DROP INDEX uk_bracket_competition_category;

ALTER TABLE brackets
    ADD COLUMN atual BOOLEAN NOT NULL DEFAULT TRUE AFTER ativo;

UPDATE brackets
SET atual = TRUE;

CREATE INDEX idx_brackets_competition_category_current
    ON brackets (competition_id, category_id, atual, data_cadastro);
