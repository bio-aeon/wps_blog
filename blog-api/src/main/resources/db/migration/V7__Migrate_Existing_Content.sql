-- Migrate existing posts to English translations
INSERT INTO post_translations (post_id, language_code, name, text, short_text,
                               seo_title, seo_description, seo_keywords,
                               translation_status, created_at, updated_at)
SELECT id, 'en', name, text, short_text,
       meta_title, meta_description, meta_keywords,
       'published', created_at, CURRENT_TIMESTAMP
FROM posts;

-- Migrate existing pages to English translations
INSERT INTO page_translations (page_id, language_code, title, content,
                               translation_status, created_at, updated_at)
SELECT id, 'en', title, content,
       'published', created_at, CURRENT_TIMESTAMP
FROM pages;

-- Migrate existing tags to English translations
INSERT INTO tag_translations (tag_id, language_code, name, created_at)
SELECT id, 'en', name, CURRENT_TIMESTAMP
FROM tags;
