-- V4: Add parent_index column to notes table.
-- This stores the list-index of the parent note within the game's ordered notes list,
-- so it can be serialized to JSON without lazy-loading the entire game collection.

ALTER TABLE notes
    ADD COLUMN parent_index INT NULL;

-- Backfill parent_index for existing notes that have a parent_id,
-- using the note_index of the parent within the same game.
UPDATE notes child
    JOIN notes parent ON child.parent_id = parent.id
SET child.parent_index = parent.note_index
WHERE child.parent_id IS NOT NULL;