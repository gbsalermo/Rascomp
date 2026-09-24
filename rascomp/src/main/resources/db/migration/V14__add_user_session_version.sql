-- ETAPA 4 / BLOCO 1: controla uma sessão ativa por conta.
-- Cada novo login incrementa session_version; tokens anteriores deixam de ser aceitos.
ALTER TABLE user_accounts
    ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
