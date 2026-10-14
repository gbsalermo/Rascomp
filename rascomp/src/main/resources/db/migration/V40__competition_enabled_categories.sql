CREATE TABLE competition_enabled_categories (
 competition_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 PRIMARY KEY (competition_id, category_id),
 CONSTRAINT fk_cec_comp FOREIGN KEY (competition_id) REFERENCES competitions(id),
 CONSTRAINT fk_cec_cat FOREIGN KEY (category_id) REFERENCES competition_categories(id)
) ENGINE=InnoDB;

-- Preserve historical registrations: previously used categories remain enabled.
INSERT IGNORE INTO competition_enabled_categories(competition_id,category_id)
SELECT DISTINCT competition_id,category_id FROM registrations;

-- Future football registrations can be collected before match logic is implemented.
INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro)
SELECT 'Futebol de Robôs','Inscrição da categoria Futebol de Robôs; partidas em desenvolvimento','FUTEBOL',TRUE,NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM competition_categories WHERE modalidade='FUTEBOL');
