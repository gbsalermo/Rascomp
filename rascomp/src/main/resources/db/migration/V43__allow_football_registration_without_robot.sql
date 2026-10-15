-- Futebol pode receber inscrições antes da atribuição do robô pela organização.
-- Para Sumô e Follow Line, robot_id continua obrigatório pelas regras da aplicação.
-- A FK e a restrição UNIQUE existentes permanecem intactas.
ALTER TABLE registrations
    MODIFY COLUMN robot_id BIGINT NULL;
