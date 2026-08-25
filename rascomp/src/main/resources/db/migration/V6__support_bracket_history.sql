-- MySQL DDL is auto-committed. Keep this migration idempotent so a failed
-- local execution can be repaired and safely retried without manual schema edits.

SET @rascomp_schema = DATABASE();

SET @drop_legacy_unique = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @rascomp_schema
          AND table_name = 'brackets'
          AND index_name = 'uk_bracket_competition_category'
    ),
    'ALTER TABLE brackets DROP INDEX uk_bracket_competition_category',
    'SELECT 1'
);
PREPARE rascomp_stmt FROM @drop_legacy_unique;
EXECUTE rascomp_stmt;
DEALLOCATE PREPARE rascomp_stmt;

SET @add_current_column = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @rascomp_schema
          AND table_name = 'brackets'
          AND column_name = 'atual'
    ),
    'SELECT 1',
    'ALTER TABLE brackets ADD COLUMN atual BOOLEAN NOT NULL DEFAULT TRUE AFTER ativo'
);
PREPARE rascomp_stmt FROM @add_current_column;
EXECUTE rascomp_stmt;
DEALLOCATE PREPARE rascomp_stmt;

UPDATE brackets
SET atual = TRUE
WHERE atual IS NULL OR atual = FALSE;

SET @add_history_index = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @rascomp_schema
          AND table_name = 'brackets'
          AND index_name = 'idx_brackets_competition_category_current'
    ),
    'SELECT 1',
    'CREATE INDEX idx_brackets_competition_category_current ON brackets (competition_id, category_id, atual, data_cadastro)'
);
PREPARE rascomp_stmt FROM @add_history_index;
EXECUTE rascomp_stmt;
DEALLOCATE PREPARE rascomp_stmt;
