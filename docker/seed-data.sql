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

-- Insert a sample page
INSERT INTO pages (url, title, content)
VALUES (
  'about',
  'About This Blog',
  '<p>This is a personal blog and technology playground demonstrating modern software engineering practices across multiple technology stacks including Scala, Rust, and Python.</p>'
) ON CONFLICT (url) DO NOTHING;
