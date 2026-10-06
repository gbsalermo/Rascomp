ALTER TABLE user_accounts
    ADD COLUMN email_verified_at DATETIME NULL AFTER email;

UPDATE user_accounts
SET email_verified_at = CURRENT_TIMESTAMP
WHERE email_verified_at IS NULL;

CREATE TABLE account_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_account_tokens_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_account_tokens_user_type_created
    ON account_tokens(user_account_id, type, created_at);
