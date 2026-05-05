-- Add note_id column to note_reactions and backfill from note_index
ALTER TABLE note_reactions
  ADD COLUMN note_id BIGINT NULL;

-- Backfill note_id by joining on game_id and note_index (only works if notes.note_index matches)
UPDATE note_reactions nr
JOIN notes n ON nr.game_id = n.game_id AND nr.note_index = n.note_index
SET nr.note_id = n.id;

-- Add foreign key constraint
ALTER TABLE note_reactions
  ADD CONSTRAINT fk_note_reactions_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE;

-- Add unique constraint on user/game/note
ALTER TABLE note_reactions
  ADD UNIQUE KEY ux_note_reactions_user_game_note (user_id, game_id, note_id);
