-- Add is_approved column for comment moderation workflow
ALTER TABLE comments ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT TRUE;

-- Index for filtering by approval status
CREATE INDEX idx_comments_is_approved ON comments(is_approved);
