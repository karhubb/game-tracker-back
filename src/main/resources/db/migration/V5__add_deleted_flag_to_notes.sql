-- Mark notes that stay in the thread tree after their content is removed.
ALTER TABLE notes
  ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
