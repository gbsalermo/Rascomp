-- Persistent, atomic reservation of R2 capacity.
-- All amounts are decimal bytes, conservatively below the R2 10 GB-month free allowance.
CREATE TABLE r2_storage_quota (
    category VARCHAR(24) PRIMARY KEY,
    limit_bytes BIGINT NOT NULL,
    used_bytes BIGINT NOT NULL DEFAULT 0
);
INSERT INTO r2_storage_quota (category, limit_bytes, used_bytes) VALUES
    ('competition', 2700000000, 0),
    ('gallery', 5400000000, 0);

CREATE TABLE r2_stored_object (
    object_key VARCHAR(512) PRIMARY KEY,
    category VARCHAR(24) NOT NULL,
    size_bytes BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_r2_object_quota FOREIGN KEY (category) REFERENCES r2_storage_quota(category)
);
CREATE INDEX idx_r2_object_state ON r2_stored_object(state);
