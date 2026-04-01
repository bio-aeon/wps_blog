-- Content Assistant: enums, tables, indexes, triggers

CREATE TYPE content_platform AS ENUM ('blog', 'linkedin', 'twitter');
CREATE TYPE draft_status AS ENUM (
    'idea', 'generating', 'draft', 'review',
    'approved', 'published', 'archived'
);

-- Central entity: drafts for all platforms
CREATE TABLE content_drafts (
    id             SERIAL PRIMARY KEY,
    platform       content_platform NOT NULL,
    title          TEXT,
    body           TEXT NOT NULL DEFAULT '',
    status         draft_status NOT NULL DEFAULT 'draft',
    source_post_id INTEGER REFERENCES posts(id) ON DELETE SET NULL,
    language_code  VARCHAR(5) REFERENCES languages(code) DEFAULT 'en',
    metadata       JSONB NOT NULL DEFAULT '{}',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Reusable prompt templates per platform
CREATE TABLE content_templates (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    platform        content_platform NOT NULL,
    prompt_template TEXT NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit log for all LLM interactions
CREATE TABLE generation_history (
    id          SERIAL PRIMARY KEY,
    draft_id    INTEGER REFERENCES content_drafts(id) ON DELETE CASCADE,
    prompt      TEXT NOT NULL,
    model       VARCHAR(100) NOT NULL,
    response    TEXT NOT NULL,
    token_usage JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX idx_content_drafts_platform        ON content_drafts(platform);
CREATE INDEX idx_content_drafts_status          ON content_drafts(status);
CREATE INDEX idx_content_drafts_platform_status ON content_drafts(platform, status);
CREATE INDEX idx_content_drafts_source_post     ON content_drafts(source_post_id)
    WHERE source_post_id IS NOT NULL;
CREATE INDEX idx_content_drafts_language        ON content_drafts(language_code);
CREATE INDEX idx_content_drafts_created         ON content_drafts(created_at DESC);
CREATE INDEX idx_content_templates_platform     ON content_templates(platform);
CREATE INDEX idx_generation_history_draft       ON generation_history(draft_id);
CREATE INDEX idx_generation_history_created     ON generation_history(created_at DESC);

-- Auto-update updated_at on content_drafts
CREATE OR REPLACE FUNCTION update_content_drafts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER content_drafts_updated_at_trigger
    BEFORE UPDATE ON content_drafts
    FOR EACH ROW
    EXECUTE FUNCTION update_content_drafts_updated_at();
