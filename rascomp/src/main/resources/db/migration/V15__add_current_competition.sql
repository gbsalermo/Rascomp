-- ETAPA 4 / BLOCO 2.2: competição vigente escolhida explicitamente pelo DEV.
ALTER TABLE competitions
    ADD COLUMN vigente BOOLEAN NOT NULL DEFAULT FALSE;

-- Preserva um contexto útil em bancos já existentes.
-- A partir desta migration, novas trocas são explícitas pelo DEV.
UPDATE competitions
SET vigente = CASE
    WHEN id = (
        SELECT escolhido.id
        FROM (
            SELECT id
            FROM competitions
            WHERE ativo = TRUE
            ORDER BY
                CASE status
                    WHEN 'EM_ANDAMENTO' THEN 1
                    WHEN 'INSCRICOES_ABERTAS' THEN 2
                    WHEN 'INSCRICOES_ENCERRADAS' THEN 3
                    WHEN 'PLANEJADA' THEN 4
                    WHEN 'FINALIZADA' THEN 5
                    ELSE 6
                END,
                data_inicio DESC,
                id DESC
            LIMIT 1
        ) escolhido
    ) THEN TRUE
    ELSE FALSE
END;
