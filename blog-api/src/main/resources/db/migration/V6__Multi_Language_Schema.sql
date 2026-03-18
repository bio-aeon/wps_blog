-- ============================================================
-- Languages reference table
-- ============================================================
CREATE TABLE languages (
    code        VARCHAR(5)  PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    native_name VARCHAR(50) NOT NULL,
    is_default  BOOLEAN     DEFAULT FALSE,
    is_active   BOOLEAN     DEFAULT TRUE,
    sort_order  INTEGER     DEFAULT 0
);

INSERT INTO languages (code, name, native_name, is_default, is_active, sort_order) VALUES
    ('en', 'English',  'English',    TRUE,  TRUE, 1),
    ('ru', 'Russian',  'Русский',    FALSE, TRUE, 2),
    ('el', 'Greek',    'Ελληνικά',   FALSE, TRUE, 3);

-- Enforce exactly one default language
CREATE UNIQUE INDEX idx_languages_single_default
    ON languages (is_default) WHERE is_default = TRUE;

-- ============================================================
-- Post translations
-- ============================================================
CREATE TABLE post_translations (
    id                  SERIAL       PRIMARY KEY,
    post_id             INTEGER      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    language_code       VARCHAR(5)   NOT NULL REFERENCES languages(code),
    name                TEXT         NOT NULL,
    text                TEXT,
    short_text          TEXT,
    seo_title           VARCHAR(255),
    seo_description     TEXT,
    seo_keywords        TEXT,
    translation_status  VARCHAR(20)  DEFAULT 'draft',
    search_vector       tsvector,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, language_code)
);

CREATE INDEX idx_post_translations_lang     ON post_translations(language_code);
CREATE INDEX idx_post_translations_post     ON post_translations(post_id);
CREATE INDEX idx_post_translations_status   ON post_translations(translation_status);
CREATE INDEX idx_post_translations_search   ON post_translations USING GIN(search_vector);

-- Language-aware full-text search trigger
CREATE OR REPLACE FUNCTION post_translations_search_trigger() RETURNS trigger AS $$
DECLARE
    ts_config TEXT;
BEGIN
    ts_config := CASE NEW.language_code
        WHEN 'en' THEN 'english'
        WHEN 'ru' THEN 'russian'
        WHEN 'el' THEN 'greek'
        ELSE 'simple'
    END;
    NEW.search_vector := to_tsvector(
        ts_config::regconfig,
        coalesce(NEW.name, '') || ' ' || coalesce(NEW.text, '')
    );
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER post_translations_search_update
    BEFORE INSERT OR UPDATE ON post_translations
    FOR EACH ROW EXECUTE FUNCTION post_translations_search_trigger();

-- ============================================================
-- Page translations
-- ============================================================
CREATE TABLE page_translations (
    id                  SERIAL       PRIMARY KEY,
    page_id             INTEGER      NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    language_code       VARCHAR(5)   NOT NULL REFERENCES languages(code),
    title               TEXT         NOT NULL,
    content             TEXT,
    seo_title           VARCHAR(255),
    seo_description     TEXT,
    translation_status  VARCHAR(20)  DEFAULT 'draft',
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(page_id, language_code)
);

CREATE INDEX idx_page_translations_lang ON page_translations(language_code);
CREATE INDEX idx_page_translations_page ON page_translations(page_id);

-- ============================================================
-- Tag translations
-- ============================================================
CREATE TABLE tag_translations (
    id              SERIAL      PRIMARY KEY,
    tag_id          INTEGER     NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    language_code   VARCHAR(5)  NOT NULL REFERENCES languages(code),
    name            TEXT        NOT NULL,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tag_id, language_code)
);

CREATE INDEX idx_tag_translations_lang ON tag_translations(language_code);
CREATE INDEX idx_tag_translations_tag  ON tag_translations(tag_id);
