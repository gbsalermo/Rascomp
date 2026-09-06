ALTER TABLE competition_categories
    ADD COLUMN sumo_physical_class VARCHAR(20) NULL AFTER modalidade;

UPDATE competition_categories c
JOIN config_sumo s ON s.competition_category_id = c.id
SET c.sumo_physical_class = CASE
    WHEN s.peso_max <= 0.500 THEN 'MINI_500G'
    ELSE 'SUMO_3KG'
END
WHERE c.modalidade = 'SUMO'
  AND c.sumo_physical_class IS NULL;
