CREATE TABLE help_document (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 type VARCHAR(32) NOT NULL,
 title VARCHAR(180) NOT NULL,
 description VARCHAR(1000),
 scope_competition_id BIGINT NULL,
 filename VARCHAR(255) NOT NULL,
 storage_key VARCHAR(500) NOT NULL,
 mime_type VARCHAR(80) NOT NULL DEFAULT 'application/pdf',
 version INT NOT NULL DEFAULT 1,
 published BOOLEAN NOT NULL DEFAULT FALSE,
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_help_document_competition FOREIGN KEY (scope_competition_id) REFERENCES competition(id)
);
CREATE INDEX idx_help_document_public ON help_document(published,type,scope_competition_id);
