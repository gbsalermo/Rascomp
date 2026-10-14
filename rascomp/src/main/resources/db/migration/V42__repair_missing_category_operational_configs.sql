-- Complete operational configuration for catalog entries added in V40/V41.
-- Existing configurations are never changed. Follow values use the validated test
-- baseline (120 seconds, 5 checkpoints) until the organizer configures the course.
INSERT INTO config_follow
(competition_category_id, numero_tomadas, tentativas_por_tomada, max_tempo_segundos,
 numero_checkpoints, penalidade_padrao_segundos, tempo_apresentacao_segundos)
SELECT c.id, 3, 3, 120, 5, 10, 60
FROM competition_categories c
LEFT JOIN config_follow f ON f.competition_category_id=c.id
WHERE c.modalidade='FOLLOW_LINE' AND c.ativo=TRUE AND f.id IS NULL;

-- Use the existing RRC Sumô rules for newly seeded categories only.
INSERT INTO config_sumo
(competition_category_id, peso_max, exige_inspecao, max_tentativas_inspecao,
 numero_rounds, rounds_para_vencer, permite_round_desempate, max_rounds_extras)
SELECT c.id,
 CASE WHEN c.sumo_physical_class='MINI_500G' THEN 0.500 ELSE 3.000 END,
 TRUE, 3, 3, 2, TRUE, 2
FROM competition_categories c
LEFT JOIN config_sumo s ON s.competition_category_id=c.id
WHERE c.modalidade='SUMO'
  AND c.ativo=TRUE
  AND c.sumo_physical_class IN ('MINI_500G','SUMO_3KG')
  AND s.id IS NULL;
