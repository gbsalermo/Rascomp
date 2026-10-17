-- Inscrição pessoal independe de associação prévia à equipe.
-- A associação poderá ser concluída posteriormente, preservando o mesmo competidor.
ALTER TABLE competitors MODIFY COLUMN team_id BIGINT NULL;
