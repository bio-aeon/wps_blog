-- Full-text search support for posts table

-- Add tsvector column for full-text search
ALTER TABLE posts ADD COLUMN search_vector tsvector;

-- Populate search vector with weighted fields
-- A = highest weight (title), B = medium (short text), C = lowest (full text)
UPDATE posts SET search_vector =
  setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
  setweight(to_tsvector('english', coalesce(short_text, '')), 'B') ||
  setweight(to_tsvector('english', coalesce(text, '')), 'C');

-- Create GIN index for fast full-text search
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);

-- Trigger function to auto-update search vector on insert/update
CREATE OR REPLACE FUNCTION posts_search_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(NEW.short_text, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(NEW.text, '')), 'C');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Create trigger to call function before insert or update
CREATE TRIGGER posts_search_update
  BEFORE INSERT OR UPDATE ON posts
  FOR EACH ROW EXECUTE FUNCTION posts_search_trigger();

-- Performance indexes for common queries
CREATE INDEX idx_posts_is_hidden ON posts(is_hidden);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
