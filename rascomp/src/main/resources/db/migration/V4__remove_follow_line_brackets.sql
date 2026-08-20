-- FOLLOW_LINE é disputado por tentativas e ranking, não por chaveamento.
-- Remove dados legados criados antes da regra de domínio ser endurecida.

DELETE rs
FROM rounds_sumo rs
JOIN matches m ON m.id = rs.match_id
JOIN brackets b ON b.id = m.bracket_id
JOIN competition_categories c ON c.id = b.category_id
WHERE c.modalidade = 'FOLLOW_LINE';

DELETE mr
FROM match_results mr
JOIN matches m ON m.id = mr.match_id
JOIN brackets b ON b.id = m.bracket_id
JOIN competition_categories c ON c.id = b.category_id
WHERE c.modalidade = 'FOLLOW_LINE';

DELETE m
FROM matches m
JOIN brackets b ON b.id = m.bracket_id
JOIN competition_categories c ON c.id = b.category_id
WHERE c.modalidade = 'FOLLOW_LINE';

DELETE b
FROM brackets b
JOIN competition_categories c ON c.id = b.category_id
WHERE c.modalidade = 'FOLLOW_LINE';
