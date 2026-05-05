-- Flyway migration: create notes table to hold GameNote entities
-- Adjust types as needed for your MySQL version on Aiven
CREATE TABLE IF NOT EXISTS notes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  content TEXT,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  date TIMESTAMP,
  author_username VARCHAR(255),
  note_index INT,
  CONSTRAINT fk_notes_game FOREIGN KEY (game_id) REFERENCES juegos(id) ON DELETE CASCADE,
  CONSTRAINT fk_notes_parent FOREIGN KEY (parent_id) REFERENCES notes(id) ON DELETE CASCADE,
  INDEX idx_notes_game (game_id),
  INDEX idx_notes_parent (parent_id)
);
