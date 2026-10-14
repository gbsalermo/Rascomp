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

-- Make the existing supported modalities selectable on an empty production catalog.
INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro,sumo_physical_class,sumo_control_mode)
SELECT 'Follow Line','Seguidor de linha','FOLLOW_LINE',TRUE,NOW(6),NULL,NULL
WHERE NOT EXISTS(SELECT 1 FROM competition_categories WHERE modalidade='FOLLOW_LINE');

INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro,sumo_physical_class,sumo_control_mode)
SELECT 'Mini Sumô Autônomo','Mini Sumô 500 g autônomo','SUMO',TRUE,NOW(6),'MINI_500G','AUTONOMO'
WHERE NOT EXISTS(SELECT 1 FROM competition_categories WHERE sumo_physical_class='MINI_500G' AND sumo_control_mode='AUTONOMO');

INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro,sumo_physical_class,sumo_control_mode)
SELECT 'Mini Sumô RC','Mini Sumô 500 g radiocontrolado','SUMO',TRUE,NOW(6),'MINI_500G','RC'
WHERE NOT EXISTS(SELECT 1 FROM competition_categories WHERE sumo_physical_class='MINI_500G' AND sumo_control_mode='RC');

INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro,sumo_physical_class,sumo_control_mode)
SELECT 'Sumô 3 kg Autônomo','Sumô 3 kg autônomo','SUMO',TRUE,NOW(6),'SUMO_3KG','AUTONOMO'
WHERE NOT EXISTS(SELECT 1 FROM competition_categories WHERE sumo_physical_class='SUMO_3KG' AND sumo_control_mode='AUTONOMO');

INSERT INTO competition_categories(nome,descricao,modalidade,ativo,data_cadastro,sumo_physical_class,sumo_control_mode)
SELECT 'Sumô 3 kg RC','Sumô 3 kg radiocontrolado','SUMO',TRUE,NOW(6),'SUMO_3KG','RC'
WHERE NOT EXISTS(SELECT 1 FROM competition_categories WHERE sumo_physical_class='SUMO_3KG' AND sumo_control_mode='RC');
