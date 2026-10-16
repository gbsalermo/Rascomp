CREATE TABLE competition_pix_payment (
 competition_id BIGINT NOT NULL PRIMARY KEY,
 holder_name VARCHAR(180) NOT NULL,
 pix_key VARCHAR(255) NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_competition_pix_payment_competition FOREIGN KEY (competition_id) REFERENCES competitions(id)
);