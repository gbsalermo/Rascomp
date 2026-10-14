-- CMS editorial persistente. A publicacao e um snapshot atomico, separado do rascunho.
-- Identificadores dos objetos R2 sao gerados somente no servidor.
CREATE TABLE media_asset (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 file_name VARCHAR(255) NOT NULL,
 content_type VARCHAR(80) NOT NULL,
 bytes_size BIGINT NOT NULL,
 object_key VARCHAR(512) NOT NULL UNIQUE,
 gallery_draft BOOLEAN NOT NULL DEFAULT FALSE,
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE media_album (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(160) NOT NULL,
 description VARCHAR(1000) NOT NULL DEFAULT '',
 cover_asset_id BIGINT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE media_album_asset (
 album_id BIGINT NOT NULL,
 asset_id BIGINT NOT NULL,
 PRIMARY KEY (album_id,asset_id),
 CONSTRAINT fk_media_album_link FOREIGN KEY (album_id) REFERENCES media_album(id) ON DELETE CASCADE,
 CONSTRAINT fk_media_asset_link FOREIGN KEY (asset_id) REFERENCES media_asset(id) ON DELETE CASCADE
);
CREATE TABLE media_site_state (
 id INT PRIMARY KEY,
 draft_editorial LONGTEXT NULL,
 published_editorial LONGTEXT NULL,
 draft_featured LONGTEXT NULL,
 published_gallery LONGTEXT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 published_at TIMESTAMP NULL
);
INSERT INTO media_site_state (id,draft_featured) VALUES (1,'[]');
-- Lista dos objetos aprovados pela publicacao atual, tambem usados pela Landing
-- mesmo quando NAO sao publicos na Galeria. Endpoint de bytes consulta esta tabela.
CREATE TABLE media_published_asset (
 asset_id BIGINT PRIMARY KEY,
 CONSTRAINT fk_media_published_asset FOREIGN KEY (asset_id) REFERENCES media_asset(id) ON DELETE CASCADE
);
CREATE INDEX idx_media_asset_gallery_draft ON media_asset(gallery_draft);
