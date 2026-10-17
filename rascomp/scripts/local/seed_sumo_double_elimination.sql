-- APENAS DESENVOLVIMENTO LOCAL. NUNCA execute contra Aiven/produção.
-- MySQL 8. Executar após Flyway, no banco LOCAL.
-- Exemplo: mysql -u root -p rascomp_local < rascomp/scripts/local/seed_sumo_double_elimination.sql
-- A proteção abaixo exige uma confirmação explícita e recusa nomes de banco de produção.
SET @CONFIRM_LOCAL_SEED = 'EU_CONFIRM0_BANCO_LOCAL';
DELIMITER $$
DROP PROCEDURE IF EXISTS seed_local_double_elimination$$
CREATE PROCEDURE seed_local_double_elimination()
BEGIN
  DECLARE v_db VARCHAR(128);
  DECLARE v_institution BIGINT;
  DECLARE v_competition BIGINT;
  DECLARE v_category BIGINT;
  DECLARE v_team BIGINT;
  DECLARE v_competitor BIGINT;
  DECLARE v_robot BIGINT;
  DECLARE v_registration BIGINT;
  DECLARE v_size INT;
  DECLARE v_i INT;
  DECLARE v_suffix VARCHAR(12);
  SET v_db = DATABASE();
  IF @CONFIRM_LOCAL_SEED <> 'EU_CONFIRM0_BANCO_LOCAL'
     OR v_db IS NULL
     OR LOWER(v_db) REGEXP 'prod|aiven|cloud|homolog' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Seed permitido somente em banco local confirmado';
  END IF;
  INSERT INTO institutions(nome,sigla,cidade,estado,ativo)
    VALUES ('Instituicao Teste Chave Dupla','TCDLOCAL','Local','BA',TRUE)
    ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
  SET v_institution=LAST_INSERT_ID();
  SET v_size=4;
  WHILE v_size <= 8 DO
    IF v_size <> 6 AND v_size <> 7 THEN
      SET v_suffix=CONCAT('TCD',v_size);
      -- Falha explicitamente em segunda execução para evitar duplicações silenciosas.
      IF EXISTS(SELECT 1 FROM competitions WHERE nome=CONCAT('[LOCAL TESTE] Dupla eliminacao ',v_size)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Seed ja executado: remova os dados locais de teste antes de repetir';
      END IF;
      INSERT INTO competitions(nome,descricao,inicio_inscricoes,fim_inscricoes,data_inicio,data_fim,status,ativo,vigente)
        VALUES(CONCAT('[LOCAL TESTE] Dupla eliminacao ',v_size),'Massa ficticia somente local',
          CURDATE()-INTERVAL 7 DAY,CURDATE()+INTERVAL 7 DAY,
          CURDATE()+INTERVAL 8 DAY,CURDATE()+INTERVAL 9 DAY,'INSCRICOES_ABERTAS',TRUE,FALSE);
      SET v_competition=LAST_INSERT_ID();
      INSERT INTO competition_categories(nome,descricao,modalidade,sumo_physical_class,sumo_control_mode,ativo)
        VALUES(CONCAT('[LOCAL TESTE] Mini Sumo ',v_size),'Somente desenvolvimento','SUMO','MINI_500G','RC',TRUE);
      SET v_category=LAST_INSERT_ID();
      INSERT INTO config_sumo(competition_category_id,peso_max,exige_inspecao,max_tentativas_inspecao,numero_rounds,rounds_para_vencer,permite_round_desempate,max_rounds_extras)
        VALUES(v_category,0.500,FALSE,1,3,2,TRUE,2);
      INSERT INTO competition_enabled_categories(competition_id,category_id) VALUES(v_competition,v_category);
      SET v_i=1;
      WHILE v_i <= v_size DO
        INSERT INTO teams(nome,institution_id,ativo)
          VALUES(CONCAT('[LOCAL ',v_suffix,'] Equipe ',v_i),v_institution,TRUE);
        SET v_team=LAST_INSERT_ID();
        INSERT INTO competitors(nome,email,team_id,ativo)
          VALUES(CONCAT('[LOCAL ',v_suffix,'] Competidor ',v_i),
                 CONCAT('tcd_',v_size,'_',v_i,'@example.invalid'),v_team,TRUE);
        SET v_competitor=LAST_INSERT_ID();
        INSERT INTO robots(nome,descricao,team_id,ativo)
          VALUES(CONCAT('[LOCAL ',v_suffix,'] Robo ',v_i),'Robo ficticio para chave dupla',v_team,TRUE);
        SET v_robot=LAST_INSERT_ID();
        INSERT INTO robot_responsibles(robot_id,competitor_id,ativo)
          VALUES(v_robot,v_competitor,TRUE);
        INSERT INTO registrations(competition_id,category_id,team_id,robot_id,status,ativo)
          VALUES(v_competition,v_category,v_team,v_robot,'APROVADA',TRUE);
        SET v_registration=LAST_INSERT_ID();
        INSERT INTO registration_competitors(registration_id,competitor_id)
          VALUES(v_registration,v_competitor);
        SET v_i=v_i+1;
      END WHILE;
      SELECT v_size AS competidores,v_competition AS competition_id,v_category AS category_id,
             'LOCAL TESTE: selecione esta competicao na Gestao' AS instrucao;
    END IF;
    SET v_size=v_size+1;
  END WHILE;
END$$
DELIMITER ;
CALL seed_local_double_elimination();
DROP PROCEDURE seed_local_double_elimination;
