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
