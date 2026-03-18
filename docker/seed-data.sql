-- Seed data for local development
-- This script is idempotent: safe to run multiple times

-- Insert sample user (password is bcrypt hash of "password123")
INSERT INTO users (username, email, password, is_active, is_admin)
VALUES (
  'admin',
  'admin@example.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  TRUE,
  TRUE
) ON CONFLICT (username) DO NOTHING;

-- Insert sample tags
INSERT INTO tags (name, slug) VALUES
  ('Scala', 'scala'),
  ('Rust', 'rust'),
  ('Functional Programming', 'functional-programming'),
  ('Web Development', 'web-development'),
  ('DevOps', 'devops')
ON CONFLICT (slug) DO NOTHING;

-- Insert sample posts (is_hidden = FALSE so they are visible)
INSERT INTO posts (name, short_text, text, author_id, views, meta_title, meta_keywords, meta_description, is_hidden)
SELECT
  'Getting Started with Scala and HTTP4s',
  'A practical guide to building REST APIs with Scala, HTTP4s, and Cats Effect. Learn how to leverage functional programming for robust backend services.',
  '<h2>Introduction</h2>
<p>HTTP4s is a minimal, idiomatic Scala interface for HTTP services. Built on top of Cats Effect and fs2, it provides a purely functional approach to building web applications.</p>
<h2>Setting Up Your Project</h2>
<p>To get started, you will need sbt and a JDK 17+ installation. Create a new project with the http4s-ember-server dependency.</p>
<h2>Defining Routes</h2>
<p>Routes in HTTP4s are defined as partial functions from Request to Response, wrapped in the HttpRoutes type. This allows for composable, type-safe routing.</p>
<h2>Working with JSON</h2>
<p>Using Circe for JSON encoding and decoding gives you automatic derivation of codecs from case classes, reducing boilerplate significantly.</p>
<h2>Conclusion</h2>
<p>HTTP4s provides a solid foundation for building production-ready APIs with Scala. Its functional approach leads to more predictable and testable code.</p>',
  u.id, 42,
  'Getting Started with Scala and HTTP4s',
  'scala, http4s, functional programming, rest api',
  'Learn how to build REST APIs with Scala and HTTP4s using functional programming principles.',
  FALSE
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM posts WHERE name = 'Getting Started with Scala and HTTP4s');

INSERT INTO posts (name, short_text, text, author_id, views, meta_title, meta_keywords, meta_description, is_hidden)
SELECT
  'Building Reactive UIs with Leptos and Rust',
  'Explore how Leptos brings fine-grained reactivity to Rust web development through WebAssembly compilation and server-side rendering.',
  '<h2>Why Leptos?</h2>
<p>Leptos is a full-stack Rust framework that compiles to WebAssembly. It offers fine-grained reactivity similar to SolidJS, but with Rust''s type safety and performance.</p>
<h2>Signals and Reactivity</h2>
<p>At the core of Leptos is its signal system. Signals are reactive primitives that automatically track their dependencies and update only the specific DOM nodes that need to change.</p>
<h2>Server-Side Rendering</h2>
<p>Leptos supports SSR out of the box with hydration. Your Rust code runs on the server to generate initial HTML, then the WASM bundle takes over on the client for interactivity.</p>
<h2>Component Patterns</h2>
<p>Components in Leptos use the #[component] macro and return impl IntoView. Props are passed as function arguments, making the API feel natural to Rust developers.</p>
<h2>Conclusion</h2>
<p>Leptos represents an exciting direction for web development, bringing Rust''s safety guarantees to the frontend while maintaining excellent performance.</p>',
  u.id, 87,
  'Building Reactive UIs with Leptos and Rust',
  'rust, leptos, webassembly, frontend, reactive',
  'Discover how to build fast, type-safe web interfaces with Leptos and Rust compiled to WebAssembly.',
  FALSE
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM posts WHERE name = 'Building Reactive UIs with Leptos and Rust');

INSERT INTO posts (name, short_text, text, author_id, views, meta_title, meta_keywords, meta_description, is_hidden)
SELECT
  'Docker Compose for Multi-Service Development',
  'How to orchestrate multiple services locally with Docker Compose for a seamless development experience across different technology stacks.',
  '<h2>The Multi-Service Challenge</h2>
<p>Modern applications often consist of multiple services written in different languages. Coordinating databases, backends, and frontends locally can be tedious without proper tooling.</p>
<h2>Docker Compose Basics</h2>
<p>Docker Compose lets you define multi-container applications in a single YAML file. Each service gets its own container with defined dependencies, networks, and volumes.</p>
<h2>Health Checks</h2>
<p>Proper health checks ensure services start in the right order. Use depends_on with condition: service_healthy to coordinate startup sequences.</p>
<h2>Seed Data</h2>
<p>For local development, having realistic sample data is essential. You can use init containers or postgres entrypoint scripts to populate your database automatically.</p>
<h2>Conclusion</h2>
<p>Docker Compose transforms the local development experience by making it trivial to spin up your entire stack with a single command.</p>',
  u.id, 15,
  'Docker Compose for Multi-Service Development',
  'docker, docker-compose, devops, local development',
  'Learn how to use Docker Compose to orchestrate multiple services for local development.',
  FALSE
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM posts WHERE name = 'Docker Compose for Multi-Service Development');

-- Link posts to tags
INSERT INTO posts_tags (post_id, tag_id)
SELECT p.id, t.id FROM posts p, tags t
WHERE (p.name = 'Getting Started with Scala and HTTP4s' AND t.slug IN ('scala', 'functional-programming', 'web-development'))
   OR (p.name = 'Building Reactive UIs with Leptos and Rust' AND t.slug IN ('rust', 'web-development'))
   OR (p.name = 'Docker Compose for Multi-Service Development' AND t.slug IN ('devops', 'web-development'))
ON CONFLICT (post_id, tag_id) DO NOTHING;

-- Insert sample comments
INSERT INTO comments (text, name, email, post_id, rating, is_approved)
SELECT
  'Great article! HTTP4s really does make API development much more pleasant with its functional approach.',
  'Reader',
  'reader@example.com',
  p.id,
  3,
  TRUE
FROM posts p WHERE p.name = 'Getting Started with Scala and HTTP4s'
AND NOT EXISTS (SELECT 1 FROM comments c WHERE c.post_id = p.id AND c.name = 'Reader');

-- Insert sample pages
INSERT INTO pages (url, title, content)
VALUES (
  'about',
  'About This Blog',
  '<p>This is a personal blog and technology playground demonstrating modern software engineering practices across multiple technology stacks including Scala, Rust, and Python.</p>'
) ON CONFLICT (url) DO NOTHING;

INSERT INTO pages (url, title, content)
VALUES (
  'contact',
  'Contact',
  '<p>Feel free to reach out via the form below or connect with me on social media.</p>'
) ON CONFLICT (url) DO NOTHING;

-- ============================================================
-- Multi-language translations
-- ============================================================

-- Tag translations (English)
INSERT INTO tag_translations (tag_id, language_code, name)
SELECT t.id, 'en', t.name FROM tags t
ON CONFLICT (tag_id, language_code) DO NOTHING;

-- Tag translations (Russian)
INSERT INTO tag_translations (tag_id, language_code, name)
SELECT t.id, 'ru', CASE t.slug
  WHEN 'scala' THEN 'Scala'
  WHEN 'rust' THEN 'Rust'
  WHEN 'functional-programming' THEN 'Функциональное программирование'
  WHEN 'web-development' THEN 'Веб-разработка'
  WHEN 'devops' THEN 'DevOps'
  ELSE t.name
END FROM tags t
ON CONFLICT (tag_id, language_code) DO NOTHING;

-- Tag translations (Greek)
INSERT INTO tag_translations (tag_id, language_code, name)
SELECT t.id, 'el', CASE t.slug
  WHEN 'scala' THEN 'Scala'
  WHEN 'rust' THEN 'Rust'
  WHEN 'functional-programming' THEN 'Συναρτησιακός Προγραμματισμός'
  WHEN 'web-development' THEN 'Ανάπτυξη Ιστού'
  WHEN 'devops' THEN 'DevOps'
  ELSE t.name
END FROM tags t
ON CONFLICT (tag_id, language_code) DO NOTHING;

-- Post translations (English) — copy from master post fields
INSERT INTO post_translations (post_id, language_code, name, text, short_text,
                               seo_title, seo_description, seo_keywords, translation_status)
SELECT p.id, 'en', p.name, p.text, p.short_text, p.meta_title, p.meta_description, p.meta_keywords, 'published'
FROM posts p
ON CONFLICT (post_id, language_code) DO NOTHING;

-- Post translations (Russian)
INSERT INTO post_translations (post_id, language_code, name, text, short_text,
                               seo_title, seo_description, seo_keywords, translation_status)
SELECT p.id, 'ru',
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN 'Начало работы со Scala и HTTP4s'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN 'Создание реактивных интерфейсов с Leptos и Rust'
    WHEN 'Docker Compose for Multi-Service Development' THEN 'Docker Compose для мультисервисной разработки'
    ELSE p.name
  END,
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN
      '<h2>Введение</h2>
<p>HTTP4s — минималистичный, идиоматичный Scala-интерфейс для HTTP-сервисов. Построенный на базе Cats Effect и fs2, он обеспечивает чисто функциональный подход к созданию веб-приложений.</p>
<h2>Настройка проекта</h2>
<p>Для начала вам понадобится sbt и JDK 17+. Создайте новый проект с зависимостью http4s-ember-server.</p>
<h2>Определение маршрутов</h2>
<p>Маршруты в HTTP4s определяются как частичные функции от Request к Response, обёрнутые в тип HttpRoutes. Это обеспечивает композируемую, типобезопасную маршрутизацию.</p>
<h2>Работа с JSON</h2>
<p>Использование Circe для кодирования и декодирования JSON обеспечивает автоматическую деривацию кодеков из case-классов, значительно сокращая шаблонный код.</p>
<h2>Заключение</h2>
<p>HTTP4s предоставляет прочную основу для создания готовых к продакшену API на Scala. Функциональный подход ведёт к более предсказуемому и тестируемому коду.</p>'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN
      '<h2>Почему Leptos?</h2>
<p>Leptos — полностековый фреймворк на Rust, компилируемый в WebAssembly. Он предлагает мелкозернистую реактивность, похожую на SolidJS, но с типобезопасностью и производительностью Rust.</p>
<h2>Сигналы и реактивность</h2>
<p>В основе Leptos лежит система сигналов. Сигналы — реактивные примитивы, которые автоматически отслеживают зависимости и обновляют только конкретные узлы DOM.</p>
<h2>Серверный рендеринг</h2>
<p>Leptos поддерживает SSR «из коробки» с гидратацией. Rust-код выполняется на сервере для генерации начального HTML, затем WASM-бандл берёт управление на клиенте.</p>
<h2>Паттерны компонентов</h2>
<p>Компоненты в Leptos используют макрос #[component] и возвращают impl IntoView. Пропсы передаются как аргументы функции, что делает API естественным для Rust-разработчиков.</p>
<h2>Заключение</h2>
<p>Leptos представляет захватывающее направление в веб-разработке, принося гарантии безопасности Rust на фронтенд.</p>'
    WHEN 'Docker Compose for Multi-Service Development' THEN
      '<h2>Проблема мультисервисности</h2>
<p>Современные приложения часто состоят из нескольких сервисов, написанных на разных языках. Координация баз данных, бэкендов и фронтендов локально может быть утомительной без подходящих инструментов.</p>
<h2>Основы Docker Compose</h2>
<p>Docker Compose позволяет определить мультиконтейнерные приложения в одном YAML-файле. Каждый сервис получает собственный контейнер с определёнными зависимостями, сетями и томами.</p>
<h2>Проверки здоровья</h2>
<p>Правильные health-чеки обеспечивают запуск сервисов в нужном порядке. Используйте depends_on с condition: service_healthy для координации последовательности запуска.</p>
<h2>Заключение</h2>
<p>Docker Compose преобразует опыт локальной разработки, делая тривиальным запуск всего стека одной командой.</p>'
    ELSE p.text
  END,
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN 'Практическое руководство по созданию REST API на Scala, HTTP4s и Cats Effect.'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN 'Как Leptos привносит мелкозернистую реактивность в веб-разработку на Rust через компиляцию в WebAssembly.'
    WHEN 'Docker Compose for Multi-Service Development' THEN 'Как оркестрировать несколько сервисов локально с Docker Compose.'
    ELSE p.short_text
  END,
  p.meta_title, p.meta_description, p.meta_keywords, 'published'
FROM posts p
ON CONFLICT (post_id, language_code) DO NOTHING;

-- Post translations (Greek)
INSERT INTO post_translations (post_id, language_code, name, text, short_text,
                               seo_title, seo_description, seo_keywords, translation_status)
SELECT p.id, 'el',
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN 'Ξεκινώντας με Scala και HTTP4s'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN 'Δημιουργία Αντιδραστικών UI με Leptos και Rust'
    WHEN 'Docker Compose for Multi-Service Development' THEN 'Docker Compose για Ανάπτυξη Πολλαπλών Υπηρεσιών'
    ELSE p.name
  END,
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN
      '<h2>Εισαγωγή</h2>
<p>Το HTTP4s είναι μια ελάχιστη, ιδιωματική διεπαφή Scala για HTTP υπηρεσίες. Χτισμένο πάνω στο Cats Effect και το fs2, παρέχει μια καθαρά συναρτησιακή προσέγγιση στη δημιουργία web εφαρμογών.</p>
<h2>Ρύθμιση του Έργου</h2>
<p>Για να ξεκινήσετε, θα χρειαστείτε sbt και εγκατάσταση JDK 17+. Δημιουργήστε ένα νέο έργο με την εξάρτηση http4s-ember-server.</p>
<h2>Ορισμός Διαδρομών</h2>
<p>Οι διαδρομές στο HTTP4s ορίζονται ως μερικές συναρτήσεις από Request σε Response, τυλιγμένες στον τύπο HttpRoutes.</p>
<h2>Εργασία με JSON</h2>
<p>Η χρήση του Circe για κωδικοποίηση και αποκωδικοποίηση JSON παρέχει αυτόματη παραγωγή codecs από case classes.</p>
<h2>Συμπέρασμα</h2>
<p>Το HTTP4s παρέχει μια σταθερή βάση για τη δημιουργία production-ready APIs με Scala.</p>'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN
      '<h2>Γιατί Leptos;</h2>
<p>Το Leptos είναι ένα full-stack Rust framework που μεταγλωττίζεται σε WebAssembly. Προσφέρει λεπτομερή αντιδραστικότητα παρόμοια με το SolidJS, αλλά με την ασφάλεια τύπων και την απόδοση της Rust.</p>
<h2>Σήματα και Αντιδραστικότητα</h2>
<p>Στον πυρήνα του Leptos βρίσκεται το σύστημα σημάτων. Τα σήματα είναι αντιδραστικά primitives που παρακολουθούν αυτόματα τις εξαρτήσεις τους.</p>
<h2>Server-Side Rendering</h2>
<p>Το Leptos υποστηρίζει SSR out of the box με hydration. Ο κώδικας Rust εκτελείται στον server για τη δημιουργία αρχικού HTML.</p>
<h2>Μοτίβα Στοιχείων</h2>
<p>Τα στοιχεία στο Leptos χρησιμοποιούν το macro #[component] και επιστρέφουν impl IntoView.</p>
<h2>Συμπέρασμα</h2>
<p>Το Leptos αντιπροσωπεύει μια συναρπαστική κατεύθυνση στην ανάπτυξη ιστού.</p>'
    WHEN 'Docker Compose for Multi-Service Development' THEN
      '<h2>Η Πρόκληση των Πολλαπλών Υπηρεσιών</h2>
<p>Οι σύγχρονες εφαρμογές συχνά αποτελούνται από πολλαπλές υπηρεσίες γραμμένες σε διαφορετικές γλώσσες.</p>
<h2>Βασικά Docker Compose</h2>
<p>Το Docker Compose σας επιτρέπει να ορίσετε εφαρμογές πολλαπλών containers σε ένα μόνο αρχείο YAML.</p>
<h2>Έλεγχοι Υγείας</h2>
<p>Οι σωστοί έλεγχοι υγείας εξασφαλίζουν ότι οι υπηρεσίες ξεκινούν με τη σωστή σειρά.</p>
<h2>Συμπέρασμα</h2>
<p>Το Docker Compose μεταμορφώνει την εμπειρία τοπικής ανάπτυξης.</p>'
    ELSE p.text
  END,
  CASE p.name
    WHEN 'Getting Started with Scala and HTTP4s' THEN 'Πρακτικός οδηγός για τη δημιουργία REST APIs με Scala, HTTP4s και Cats Effect.'
    WHEN 'Building Reactive UIs with Leptos and Rust' THEN 'Πώς το Leptos φέρνει λεπτομερή αντιδραστικότητα στην ανάπτυξη ιστού με Rust.'
    WHEN 'Docker Compose for Multi-Service Development' THEN 'Πώς να ενορχηστρώσετε πολλαπλές υπηρεσίες τοπικά με Docker Compose.'
    ELSE p.short_text
  END,
  p.meta_title, p.meta_description, p.meta_keywords, 'published'
FROM posts p
ON CONFLICT (post_id, language_code) DO NOTHING;

-- Page translations (English)
INSERT INTO page_translations (page_id, language_code, title, content, translation_status)
SELECT pg.id, 'en', pg.title, pg.content, 'published'
FROM pages pg
ON CONFLICT (page_id, language_code) DO NOTHING;

-- Page translations (Russian)
INSERT INTO page_translations (page_id, language_code, title, content, translation_status)
SELECT pg.id, 'ru',
  CASE pg.url
    WHEN 'about' THEN 'О блоге'
    WHEN 'contact' THEN 'Контакт'
    ELSE pg.title
  END,
  CASE pg.url
    WHEN 'about' THEN '<p>Это персональный блог и технологическая площадка, демонстрирующая современные практики программной инженерии на нескольких технологических стеках, включая Scala, Rust и Python.</p>'
    WHEN 'contact' THEN '<p>Свяжитесь со мной через форму ниже или найдите меня в социальных сетях.</p>'
    ELSE pg.content
  END,
  'published'
FROM pages pg
ON CONFLICT (page_id, language_code) DO NOTHING;

-- Page translations (Greek)
INSERT INTO page_translations (page_id, language_code, title, content, translation_status)
SELECT pg.id, 'el',
  CASE pg.url
    WHEN 'about' THEN 'Σχετικά με το Blog'
    WHEN 'contact' THEN 'Επικοινωνία'
    ELSE pg.title
  END,
  CASE pg.url
    WHEN 'about' THEN '<p>Αυτό είναι ένα προσωπικό blog και τεχνολογικό πεδίο πειραματισμού που επιδεικνύει σύγχρονες πρακτικές μηχανικής λογισμικού σε πολλαπλά technology stacks, συμπεριλαμβανομένων Scala, Rust και Python.</p>'
    WHEN 'contact' THEN '<p>Μη διστάσετε να επικοινωνήσετε μέσω της παρακάτω φόρμας ή να συνδεθείτε μαζί μου στα μέσα κοινωνικής δικτύωσης.</p>'
    ELSE pg.content
  END,
  'published'
FROM pages pg
ON CONFLICT (page_id, language_code) DO NOTHING;

-- Skills (for profile/about page)
INSERT INTO skills (name, slug, category, proficiency, sort_order, is_active) VALUES
  ('Rust', 'rust', 'Languages', 85, 1, TRUE),
  ('Scala', 'scala', 'Languages', 90, 2, TRUE),
  ('Python', 'python', 'Languages', 75, 3, TRUE),
  ('PostgreSQL', 'postgresql', 'Databases', 80, 1, TRUE),
  ('Leptos', 'leptos', 'Frameworks', 70, 1, TRUE),
  ('HTTP4s', 'http4s', 'Frameworks', 85, 2, TRUE)
ON CONFLICT (slug) DO NOTHING;

-- Social links
INSERT INTO social_links (platform, url, label, sort_order, is_active) VALUES
  ('github', 'https://github.com/example', 'GitHub', 1, TRUE),
  ('linkedin', 'https://linkedin.com/in/example', 'LinkedIn', 2, TRUE),
  ('email', 'mailto:admin@example.com', 'Email', 3, TRUE)
ON CONFLICT DO NOTHING;

-- Profile configs
INSERT INTO configs (name, value, comment) VALUES
  ('profile_name', 'WPS', 'Display name for about page'),
  ('profile_title', 'Software Engineer', 'Professional title'),
  ('profile_photo_url', '/assets/photo.jpg', 'Profile photo URL'),
  ('profile_resume_url', '/assets/resume.pdf', 'Resume download URL'),
  ('profile_bio', 'Engineer passionate about FP, systems programming, and building things that work.', 'Short bio text')
ON CONFLICT (name) DO NOTHING;
