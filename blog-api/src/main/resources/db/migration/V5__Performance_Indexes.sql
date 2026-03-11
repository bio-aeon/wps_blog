-- =================================================================
-- V5: Performance Indexes
-- Adding missing indexes on foreign key columns and common query
-- patterns to eliminate sequential scans.
-- =================================================================

-- Comments: post_id is used in findCommentsByPostId (every post detail page)
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);

-- Comments: parent_id is used for building comment tree (filtering root vs reply)
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);

-- Comment raters: composite for the exact hasRated query pattern
CREATE INDEX IF NOT EXISTS idx_comment_raters_comment_ip
    ON comment_raters(comment_id, ip);

-- Posts: author_id used in potential joins
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id);

-- Posts_tags: tag_id used in findByPostIds reverse lookup
CREATE INDEX IF NOT EXISTS idx_posts_tags_tag_id ON posts_tags(tag_id);

-- Posts_tags: composite covering index for the common join pattern
CREATE INDEX IF NOT EXISTS idx_posts_tags_post_tag
    ON posts_tags(post_id, tag_id);

-- Tags: slug used in findBySlug and tag page lookups
CREATE INDEX IF NOT EXISTS idx_tags_slug ON tags(slug);

-- Pages: url used in findByUrl (static page lookups)
CREATE INDEX IF NOT EXISTS idx_pages_url ON pages(url);

-- Configs: name used in findByName (config lookups)
CREATE INDEX IF NOT EXISTS idx_configs_name ON configs(name);

-- Posts: partial index for the common list query (visible posts ordered by date)
CREATE INDEX IF NOT EXISTS idx_posts_visible_created
    ON posts(created_at DESC) WHERE is_hidden = false;
