## [WIP] WPS Blog
A rewritten version of my simple blog engine, as well as my playground for the practical application of technologies that are interesting to me. The engine architecture consists of several separate services.

The previous implementation as a monolithic application on Django, is kept in [this](https://github.com/bio-aeon/wps_blog/tree/feature/docker) branch.

```
┌──────────────┐     ┌─────────────┐     ┌──────────────────┐
│   blog-ui    │────▶│  blog-api   │────▶│    PostgreSQL     │
│  (Leptos)    │     │  (Scala)    │     │  (wps_blog_db)   │
│  Port: 3000  │     │  Port: 9000 │     │  Port: 9876      │
└──────────────┘     └─────────────┘     └──────────────────┘
```

| Service | Stack | Description |
|---------|-------|-------------|
| **blog-api** | Scala, HTTP4s, Doobie, Cats Effect | REST API with Swagger docs |
| **blog-ui** | Rust, Leptos, Actix-web, WASM | SSR frontend with hydration |
| **blog-admin** | Python, Django | Content management (WIP) |

### Quick Start with Docker Compose

Launch the full stack with a single command:

```bash
docker compose up --build
```

This starts PostgreSQL, runs database migrations, seeds sample data, and launches the API and UI.

> **Note:** The first build takes a while (Scala compilation + Rust/WASM compilation). Subsequent builds use Docker layer caching and are much faster.

Once everything is up:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API | http://localhost:9000 |
| Swagger UI | http://localhost:9000/docs |
| PostgreSQL | localhost:9876 (user: `postgres`, password: `postgres`) |

### Stopping and Resetting

```bash
# Stop all services
docker compose down

# Stop and remove all data (database, volumes)
docker compose down -v
```

### Development Setup (without Docker)

#### Prerequisites

- PostgreSQL running on port 9876 (database: `wps_blog_db`)
- Rust + cargo-leptos: `cargo install cargo-leptos --locked`
- Scala + sbt
- Python 3.x (for blog-admin)

#### Running Services Individually

```bash
# Terminal 1 - API
cd code/blog-api && sbt run

# Terminal 2 - Frontend
cd code/blog-ui && cargo leptos serve

# Terminal 3 - Admin (optional)
cd code/blog-admin/blog_admin && python manage.py runserver 8000
```

### Testing

```bash
# API tests (Scala)
cd code/blog-api && sbt test

# UI tests (Rust + Playwright)
cd code/blog-ui && cargo leptos test

# Admin tests (Django)
cd code/blog-admin/blog_admin && python manage.py test
```

### To do

- [x] API service
- [x] UI service (SPA + SSR)
- [ ] Admin service
- [ ] CI using Github Actions
