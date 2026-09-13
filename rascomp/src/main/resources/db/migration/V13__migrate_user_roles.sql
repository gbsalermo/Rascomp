-- ETAPA 3: ORGANIZACAO possuia acesso administrativo integral.
-- DEV preserva esse nivel de acesso na nova matriz.
UPDATE user_accounts
SET role = 'DEV'
WHERE role = 'ORGANIZACAO';
