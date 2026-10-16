-- Estrutura aditiva: chaves históricas continuam SINGLE_ELIMINATION.
ALTER TABLE brackets ADD COLUMN bracket_format VARCHAR(32) NOT NULL DEFAULT 'SINGLE_ELIMINATION';
ALTER TABLE matches ADD COLUMN bracket_section VARCHAR(24) NOT NULL DEFAULT 'WINNERS';
ALTER TABLE matches ADD COLUMN winner_next_match_id BIGINT NULL;
ALTER TABLE matches ADD COLUMN loser_next_match_id BIGINT NULL;
ALTER TABLE matches ADD COLUMN winner_next_slot VARCHAR(1) NULL;
ALTER TABLE matches ADD COLUMN loser_next_slot VARCHAR(1) NULL;
ALTER TABLE matches ADD CONSTRAINT fk_match_winner_next FOREIGN KEY (winner_next_match_id) REFERENCES matches(id);
ALTER TABLE matches ADD CONSTRAINT fk_match_loser_next FOREIGN KEY (loser_next_match_id) REFERENCES matches(id);
