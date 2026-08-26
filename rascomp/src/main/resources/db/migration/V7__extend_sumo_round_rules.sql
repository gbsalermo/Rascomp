ALTER TABLE rounds_sumo
    ADD COLUMN motivo_resultado VARCHAR(30) NOT NULL DEFAULT 'DISPUTA' AFTER status,
    ADD COLUMN penalidades_a INT NOT NULL DEFAULT 0 AFTER motivo_resultado,
    ADD COLUMN penalidades_b INT NOT NULL DEFAULT 0 AFTER penalidades_a;
