# Phase 6: DevOps & Infrastructure — Implementation Blueprint

## Current State

### Already implemented
| Item | Status | Location |
|------|--------|----------|
| blog-api Dockerfile | Done | `blog-api/Dockerfile` (multi-stage sbt build + JRE runtime) |
| blog-ui Dockerfile | Done | `blog-ui/Dockerfile` (Rust builder + Debian slim runtime) |
| docker-compose.yml | Done | Root `docker-compose.yml` (postgres, blog-api, blog-ui, db-seed) |
| GitHub Actions — blog-api CI | Done | `.github/workflows/blog-api-ci.yml` (sbt test) |
| GitHub Actions — blog-ui CI | Done | `.github/workflows/blog-ui-ci.yml` (cargo leptos build + test) |
| GitHub Actions — blog-admin CI | Done | `.github/workflows/blog-admin-ci.yml` (Django test) |
| Health endpoint | Done | `GET /health` with DB check (HealthServiceImpl) |
| Prometheus metrics | Done | `GET /metrics` + MetricsMiddleware (Phase 5) |
| DB env var overrides (blog-api) | Done | `application.conf` uses `${?DB_URL}`, `${?DB_USERNAME}`, `${?DB_PASSWORD}` |

### Not yet implemented
| Item | Priority | Notes |
|------|----------|-------|
| blog-admin Dockerfile | P1 | No container for Django admin |
| blog-admin in docker-compose | P1 | Not included in full-stack compose |
| Production compose overrides | P2 | Single compose file, no dev/prod split |
| Environment variable externalization (blog-admin) | P1 | Hardcoded SECRET_KEY, DB creds, ALLOWED_HOSTS |
| .env templates | P1 | No .env.example for any service |
| Dependabot/Renovate | P2 | Dependencies manually managed |
| Deployment pipeline | P2 | No automated deploy workflow |
| Structured logging | P2 | Pattern-based stdout only (`logback.xml`) |
| Correlation IDs | P2 | No request tracing across logs |
| Liveness vs readiness probes | P2 | Single `/health` serves both roles |
| Backup strategy | P1 | No PostgreSQL backup mechanism |
| Migration safety in deploy | P2 | No pre-deploy migration validation |

### Deferred (P3 — out of scope)
- Distributed tracing (OpenTelemetry) — valuable but heavy for a personal blog
- Log aggregation (ELK/cloud logging) — no external log sink needed yet
- Database replication — single-instance is sufficient at current scale
- Release automation (semantic versioning, changelog generation)
- Secrets management (Vault/KMS) — env vars + Docker secrets sufficient

---

## Implementation Plan

### Batch 1: Containerization & Environment Foundation

**Goal:** Complete containerization of all services, externalize secrets, create production-ready compose configuration.

#### 1.1 blog-admin Dockerfile

**New file:** `blog-admin/Dockerfile`

Multi-stage is unnecessary for Python — use a simple single-stage build. Add `gunicorn` for production WSGI serving and `whitenoise` for static file serving without nginx.

```dockerfile
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends libpq-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY blog_admin/ .

RUN python manage.py collectstatic --noinput

RUN useradd -r -s /usr/sbin/nologin appuser
USER appuser

EXPOSE 8000

CMD ["gunicorn", "blog_admin.wsgi:application", "--bind", "0.0.0.0:8000", "--workers", "2"]
```

**Dependency changes:** `blog-admin/requirements.txt`
- Add `gunicorn==23.0.0`
- Add `whitenoise==6.8.2`

**New file:** `blog-admin/.dockerignore`
```
venv/
.idea/
.vscode/
__pycache__/
*.pyc
.gitignore
```

**Settings changes for whitenoise:** `blog-admin/blog_admin/blog_admin/settings.py`
- Add `whitenoise.middleware.WhiteNoiseMiddleware` after `SecurityMiddleware` in `MIDDLEWARE`
- Add `STATIC_ROOT = BASE_DIR / 'staticfiles'`
- Add `STORAGES = {"staticfiles": {"BACKEND": "whitenoise.storage.CompressedManifestStaticFilesStorage"}}`

**Note:** `collectstatic` requires `STATIC_ROOT`. The `whitenoise` middleware serves collected static files in production without nginx.

#### 1.2 blog-admin settings.py — Environment Variable Externalization

**Modify:** `blog-admin/blog_admin/blog_admin/settings.py`

Replace hardcoded secrets and configuration with `os.environ.get()` calls:

```python
import os

SECRET_KEY = os.environ.get('DJANGO_SECRET_KEY', 'django-insecure--tr#q^=%mh5^x7h16(@_cm_i1ukk4@(kyw@=udv4m1*yg--c3x')

DEBUG = os.environ.get('DJANGO_DEBUG', 'False').lower() in ('true', '1')

ALLOWED_HOSTS = [
    h.strip()
    for h in os.environ.get('DJANGO_ALLOWED_HOSTS', 'localhost').split(',')
    if h.strip()
]

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': os.environ.get('DB_NAME', 'wps_blog_db'),
        'USER': os.environ.get('DB_USERNAME', 'postgres'),
        'PASSWORD': os.environ.get('DB_PASSWORD', ''),
        'HOST': os.environ.get('DB_HOST', '0.0.0.0'),
        'PORT': os.environ.get('DB_PORT', '9876'),
    }
}
```

**Key decisions:**
- Use `DB_USERNAME` / `DB_PASSWORD` to match blog-api env var names
- Default `ALLOWED_HOSTS` to `localhost` (not empty, which blocks all requests when `DEBUG=False`)
- Keep insecure default SECRET_KEY for local dev only — production must override via env

#### 1.3 .env.example Template

**New file:** `.env.example` (repo root)

Single source of truth for all environment variables used across services. Developers copy this to `.env` (which is gitignored) for local configuration.

```env
# ============================================
# WPS Blog — Environment Configuration
# Copy to .env and adjust values for your setup
# ============================================

# --- PostgreSQL ---
POSTGRES_DB=wps_blog_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# --- blog-api (Scala) ---
DB_URL=jdbc:postgresql://postgres:5432/wps_blog_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# --- blog-admin (Django) ---
DJANGO_SECRET_KEY=change-me-in-production
DJANGO_DEBUG=False
DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1
DB_NAME=wps_blog_db
DB_HOST=postgres
DB_PORT=5432

# --- blog-ui (Leptos) ---
BLOG_API_URL=http://blog-api:9000
LEPTOS_SITE_ROOT=/app/site
LEPTOS_SITE_ADDR=0.0.0.0:3000
```

#### 1.4 docker-compose.yml — Add blog-admin Service

**Modify:** `docker-compose.yml`

Add `blog-admin` service after `db-seed`:

```yaml
  blog-admin:
    build:
      context: ./blog-admin
    environment:
      DB_NAME: wps_blog_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      DB_HOST: postgres
      DB_PORT: "5432"
      DJANGO_ALLOWED_HOSTS: "localhost,blog-admin"
    ports:
      - "8000:8000"
    depends_on:
      postgres:
        condition: service_healthy
```

No health check needed — blog-admin is an internal admin tool, not a dependency of other services.

#### 1.5 docker-compose.prod.yml — Production Overrides

**New file:** `docker-compose.prod.yml`

Used via `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`.

```yaml
services:
  postgres:
    restart: unless-stopped
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}

  blog-api:
    image: ghcr.io/${GITHUB_REPOSITORY:-wps}/blog-api:latest
    build: !reset null
    restart: unless-stopped
    environment:
      DB_URL: "jdbc:postgresql://postgres:5432/wps_blog_db"
      DB_USERNAME: ${DB_USERNAME:-postgres}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
    deploy:
      resources:
        limits:
          memory: 512M

  blog-ui:
    image: ghcr.io/${GITHUB_REPOSITORY:-wps}/blog-ui:latest
    build: !reset null
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 128M

  blog-admin:
    image: ghcr.io/${GITHUB_REPOSITORY:-wps}/blog-admin:latest
    build: !reset null
    restart: unless-stopped
    environment:
      DJANGO_SECRET_KEY: ${DJANGO_SECRET_KEY}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      DJANGO_ALLOWED_HOSTS: ${DJANGO_ALLOWED_HOSTS}
    deploy:
      resources:
        limits:
          memory: 128M

  db-seed:
    profiles: ["seed"]
```

**Key decisions:**
- `build: !reset null` — production uses pre-built images from GHCR, not local builds
- `db-seed` moved to `seed` profile — only run on first deployment, not every restart
- Resource limits prevent runaway memory usage on a VPS
- blog-api gets 512M (JVM), others get 128M
- Secrets come from host `.env` file via `${VAR}` interpolation

#### 1.6 .gitignore Update

**Modify:** Each service's `.gitignore` — add `.env` to repo root gitignore.

Since there's no root-level `.gitignore`, check if `.env` is already covered by any existing ignore. If not, add `.env` to `blog-api/.gitignore`, `blog-ui/.gitignore`, and `blog-admin/.gitignore`. Alternatively, create a root `.gitignore`.

Better approach: create a root-level `.gitignore`:

**New file:** `.gitignore`
```
.env
.env.local
.env.production
```

---

### Batch 2: CI/CD Enhancement

**Goal:** Automate dependency updates, Docker image builds, and deployment.

#### 2.1 Dependabot Configuration

**New file:** `.github/dependabot.yml`

```yaml
version: 2
updates:
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"

  - package-ecosystem: "pip"
    directory: "/blog-admin"
    schedule:
      interval: "weekly"

  - package-ecosystem: "cargo"
    directory: "/blog-ui"
    schedule:
      interval: "weekly"
```

**Note:** Dependabot does not support sbt natively. Scala dependency updates remain manual. An alternative is [Scala Steward](https://github.com/scala-steward-org/scala-steward), but hosting it is heavy for a personal project — omit for now.

#### 2.2 Docker Image Build & Push Workflow

**New file:** `.github/workflows/docker-publish.yml`

Builds and pushes all three service images to GitHub Container Registry (GHCR) on pushes to `master`.

```yaml
name: Build & Publish Docker Images

on:
  push:
    branches: [master]
  workflow_dispatch:

permissions:
  contents: read
  packages: write

env:
  REGISTRY: ghcr.io

jobs:
  build-api:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./blog-api
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-api:latest
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-api:${{ github.sha }}

  build-ui:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./blog-ui
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-ui:latest
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-ui:${{ github.sha }}

  build-admin:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./blog-admin
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-admin:latest
            ${{ env.REGISTRY }}/${{ github.repository }}/blog-admin:${{ github.sha }}
```

All three builds run in parallel. Each image gets both `latest` and SHA-pinned tags.

#### 2.3 Deployment Workflow

**New file:** `.github/workflows/deploy.yml`

SSH-based deployment to a single VPS — appropriate for a personal blog.

```yaml
name: Deploy

on:
  workflow_run:
    workflows: ["Build & Publish Docker Images"]
    types: [completed]
    branches: [master]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    if: ${{ github.event_name == 'workflow_dispatch' || github.event.workflow_run.conclusion == 'success' }}
    environment: production
    steps:
      - name: Deploy to server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd ${{ secrets.DEPLOY_PATH }}
            docker compose -f docker-compose.yml -f docker-compose.prod.yml pull
            docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
            docker image prune -f
```

**Required GitHub secrets:**
| Secret | Description |
|--------|-------------|
| `DEPLOY_HOST` | VPS IP or hostname |
| `DEPLOY_USER` | SSH user (e.g., `deploy`) |
| `DEPLOY_KEY` | SSH private key |
| `DEPLOY_PATH` | App directory on server (e.g., `/opt/wps-blog`) |

**Setup note:** The `production` environment in GitHub allows configuring required reviewers for manual deploy approval. Optional but recommended.

---

### Batch 3: Observability

**Goal:** Structured logging for production debugging, request correlation, and proper Kubernetes-style health probes.

#### 3.1 Structured JSON Logging (blog-api)

**Dependency:** Add `logstash-logback-encoder` for JSON-formatted log output.

**Modify:** `blog-api/project/Dependencies.scala`
- Add version: `val logstashLogbackEncoder = "8.0"`
- Add val: `val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % Versions.logstashLogbackEncoder`

**Modify:** `blog-api/build.sbt`
- Add `logstashLogbackEncoder` to `libraryDependencies`

**Replace:** `blog-api/src/main/resources/logback.xml`

Use environment variable `LOG_FORMAT` to switch between human-readable (dev) and JSON (prod):

```xml
<configuration>
  <if condition='isDefined("LOG_FORMAT") &amp;&amp; property("LOG_FORMAT").equals("json")'>
    <then>
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <includeMdcKeyName>correlationId</includeMdcKeyName>
        </encoder>
      </appender>
    </then>
    <else>
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
          <pattern>[%thread] %highlight(%-5level) %cyan(%logger{15}) - %msg %n</pattern>
        </encoder>
      </appender>
    </else>
  </if>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

**However**, logback's conditional processing requires `janino` library. A simpler alternative — use separate logback files selected by JVM system property:

**Keep:** `blog-api/src/main/resources/logback.xml` — unchanged (dev, human-readable)

**New file:** `blog-api/src/main/resources/logback-prod.xml`
```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>correlationId</includeMdcKeyName>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

**Activate in production:** pass `-Dlogback.configurationFile=logback-prod.xml` via Dockerfile ENTRYPOINT or docker-compose env:

In `docker-compose.prod.yml`, add to blog-api environment:
```yaml
JAVA_OPTS: "-Dlogback.configurationFile=logback-prod.xml"
```

And in `blog-api/Dockerfile`, change entrypoint to pass JAVA_OPTS:
```dockerfile
ENTRYPOINT ["bin/wps-blog"]
```
sbt-native-packager's `bin/wps-blog` already respects `JAVA_OPTS` environment variable — no Dockerfile change needed.

**JSON output format** (one line per log event):
```json
{"@timestamp":"2026-03-11T14:30:00.000Z","@version":"1","message":"GET /v1/posts 200","logger_name":"s.w.b.e.MetricsMiddleware","thread_name":"io-compute-1","level":"INFO","correlationId":"a1b2c3d4-..."}
```

#### 3.2 Correlation ID Middleware (blog-api)

**New file:** `blog-api/src/main/scala/su/wps/blog/endpoints/CorrelationIdMiddleware.scala`

Extracts `X-Request-Id` from the incoming request header (e.g., from a reverse proxy) or generates a UUID. Adds it to the response header for client-side correlation.

```scala
package su.wps.blog.endpoints

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.functor.*
import org.http4s.*
import org.typelevel.ci.CIString

import java.util.UUID

object CorrelationIdMiddleware {

  private val RequestIdHeader = CIString("X-Request-Id")

  def apply[F[_]: Sync](app: HttpApp[F]): HttpApp[F] =
    Kleisli { req =>
      val requestId = req.headers
        .get(RequestIdHeader)
        .map(_.head.value)
        .getOrElse(UUID.randomUUID().toString)

      app
        .run(req.putHeaders(Header.Raw(RequestIdHeader, requestId)))
        .map(_.putHeaders(Header.Raw(RequestIdHeader, requestId)))
    }
}
```

**Middleware chain update in `Program.scala`:**

Current:
```scala
MetricsMiddleware(gzipApp(CORS.policy.withAllowOriginAll(routes.orNotFound)))
```

New:
```scala
CorrelationIdMiddleware(MetricsMiddleware(gzipApp(CORS.policy.withAllowOriginAll(routes.orNotFound))))
```

CorrelationId is outermost so the ID is available for the entire request lifecycle.

**Note on MDC integration:** Full MDC-based log correlation with Cats Effect 3 fibers requires `IOLocal` and a custom `LoggerFactory`. This is complex and not worth the effort for a personal blog. The `X-Request-Id` response header provides sufficient correlation for debugging — match a user-reported request ID against server access logs. MDC integration can be added later if needed.

#### 3.3 Liveness Probe Endpoint

The current `GET /health` checks database connectivity — this is a **readiness probe** (is the app ready to serve traffic?). Kubernetes and Docker health checks also need a **liveness probe** (is the process alive and not deadlocked?).

**New file:** `blog-api/src/main/scala/su/wps/blog/endpoints/LivenessRoutes.scala`

```scala
package su.wps.blog.endpoints

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object LivenessRoutes {

  def routes[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "health" / "live" =>
      Ok("""{"status":"alive"}""")
    }
  }
}
```

**Route composition in `Program.scala`:**

```scala
val livenessRoutes = LivenessRoutes.routes[F]
val allRoutes = livenessRoutes <+> metricsRoutes <+> swaggerRoutes <+> routesWithCaching
```

**Docker health check update in `docker-compose.yml`:**

Change blog-api healthcheck to use the lightweight liveness probe:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -sf http://localhost:9000/health/live || exit 1"]
```

Keep the readiness probe (`/health` with DB check) for load balancer configuration.

#### 3.4 Tests

**New file:** `blog-api/src/test/scala/su/wps/blog/endpoints/CorrelationIdMiddlewareSpec.scala`

```
CorrelationIdMiddleware
  generates X-Request-Id when not present in request
  preserves X-Request-Id from incoming request
  adds X-Request-Id to response headers
```

Test pattern — same as `CacheMiddlewareSpec`: create a test `HttpApp`, wrap with `CorrelationIdMiddleware`, send a request, assert on response headers.

```scala
package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class CorrelationIdMiddlewareSpec extends Specification {

  private val RequestIdHeader = CIString("X-Request-Id")

  private val testApp: HttpApp[IO] = HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))

  private val app = CorrelationIdMiddleware(testApp)

  "CorrelationIdMiddleware" >> {
    "generates X-Request-Id when not present in request" >> {
      val resp = app.run(Request[IO](Method.GET, uri"/test")).unsafeRunSync()
      resp.headers.get(RequestIdHeader) must beSome
    }

    "preserves X-Request-Id from incoming request" >> {
      val customId = "test-correlation-id-123"
      val req = Request[IO](Method.GET, uri"/test")
        .putHeaders(Header.Raw(RequestIdHeader, customId))
      val resp = app.run(req).unsafeRunSync()
      resp.headers.get(RequestIdHeader).map(_.head.value) must beSome(customId)
    }

    "adds X-Request-Id to response headers" >> {
      val resp = app.run(Request[IO](Method.GET, uri"/any")).unsafeRunSync()
      val id = resp.headers.get(RequestIdHeader).map(_.head.value)
      id must beSome[String].which(_.nonEmpty)
    }
  }
}
```

**New file:** `blog-api/src/test/scala/su/wps/blog/endpoints/LivenessRoutesSpec.scala`

```
LivenessRoutes
  GET /health/live
    returns 200 OK
```

```scala
package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class LivenessRoutesSpec extends Specification {

  "LivenessRoutes" >> {
    "GET /health/live" >> {
      "returns 200 OK" >> {
        val request = Request[IO](Method.GET, uri"/health/live")
        val resp = LivenessRoutes.routes[IO]
          .run(request).value.map(_.get).unsafeRunSync()
        resp.status mustEqual Status.Ok
      }
    }
  }
}
```

---

### Batch 4: Database Operations

**Goal:** Automated PostgreSQL backups and migration safety in the deploy pipeline.

#### 4.1 PostgreSQL Backup Script

**New file:** `docker/backup.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/backups}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-wps_blog_db}"
DB_USER="${DB_USER:-postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="wps_blog_${TIMESTAMP}.sql.gz"

echo "Starting backup: ${FILENAME}"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" | gzip > "${BACKUP_DIR}/${FILENAME}"

# Rotate old backups
DELETED=$(find "$BACKUP_DIR" -name "wps_blog_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete -print | wc -l)

echo "Backup completed: ${FILENAME} (removed ${DELETED} old backups)"
```

**New file:** `docker/restore.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_FILE="${1:?Usage: restore.sh <backup-file.sql.gz>}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-wps_blog_db}"
DB_USER="${DB_USER:-postgres}"

echo "Restoring from: ${BACKUP_FILE}"
gunzip -c "$BACKUP_FILE" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME"
echo "Restore completed"
```

Both scripts are `chmod +x`.

**Add backup service to `docker-compose.prod.yml`:**

```yaml
  db-backup:
    image: postgres:18
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      PGPASSWORD: ${POSTGRES_PASSWORD}
      DB_HOST: postgres
    volumes:
      - ./docker/backup.sh:/backup.sh:ro
      - backups:/backups
    entrypoint: ["/bin/bash", "/backup.sh"]
    profiles: ["backup"]
```

Add volume:
```yaml
volumes:
  pgdata:
  backups:
```

**Usage:**
```bash
# Run one-shot backup
docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm db-backup

# Schedule daily backups via host cron
# crontab -e:
# 0 3 * * * cd /opt/wps-blog && docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm db-backup >> /var/log/wps-backup.log 2>&1
```

#### 4.2 Migration Validation in Deployment

The existing test suite already validates migrations via TestContainers (every repository spec runs Flyway against a fresh PostgreSQL). This catches broken migrations in CI before deploy.

**Enhancement:** Add a migration dry-run step to the deployment workflow that validates migrations against the production database schema without applying them.

**Modify:** `.github/workflows/deploy.yml` — add pre-deploy step:

```yaml
      - name: Validate migrations
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd ${{ secrets.DEPLOY_PATH }}
            docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm \
              -e DB_URL="jdbc:postgresql://postgres:5432/wps_blog_db" \
              blog-api bin/wps-blog --validate-migrations || exit 1
```

**Note:** This requires adding a `--validate-migrations` flag to the app. For now, rely on the CI test suite (TestContainers runs all migrations on every PR). The deploy workflow already runs after CI passes, so migration validation is implicitly covered. Add explicit validation later if needed.

---

## File Summary

### New Files (12)
| File | Batch | Description |
|------|-------|-------------|
| `blog-admin/Dockerfile` | 1 | Django container with gunicorn |
| `blog-admin/.dockerignore` | 1 | Exclude venv, IDE, cache files |
| `.env.example` | 1 | Environment variable template for all services |
| `docker-compose.prod.yml` | 1 | Production compose overrides |
| `.gitignore` | 1 | Root-level gitignore for .env files |
| `.github/dependabot.yml` | 2 | Automated dependency updates |
| `.github/workflows/docker-publish.yml` | 2 | Build & push Docker images to GHCR |
| `.github/workflows/deploy.yml` | 2 | SSH-based deployment to VPS |
| `blog-api/src/main/resources/logback-prod.xml` | 3 | JSON logging configuration |
| `blog-api/src/main/scala/su/wps/blog/endpoints/CorrelationIdMiddleware.scala` | 3 | X-Request-Id middleware |
| `blog-api/src/main/scala/su/wps/blog/endpoints/LivenessRoutes.scala` | 3 | Liveness probe at /health/live |
| `docker/backup.sh` | 4 | PostgreSQL backup script |
| `docker/restore.sh` | 4 | PostgreSQL restore script |

### Modified Files (9)
| File | Batch | Changes |
|------|-------|---------|
| `blog-admin/requirements.txt` | 1 | Add gunicorn, whitenoise |
| `blog-admin/blog_admin/blog_admin/settings.py` | 1 | Env var externalization, whitenoise middleware, STATIC_ROOT |
| `docker-compose.yml` | 1 | Add blog-admin service |
| `blog-api/project/Dependencies.scala` | 3 | Add logstash-logback-encoder version + val |
| `blog-api/build.sbt` | 3 | Add logstash-logback-encoder dependency |
| `blog-api/src/main/scala/su/wps/blog/Program.scala` | 3 | Add CorrelationIdMiddleware + LivenessRoutes to chain |

### Test Files (2)
| File | Batch | Tests |
|------|-------|-------|
| `blog-api/src/test/scala/su/wps/blog/endpoints/CorrelationIdMiddlewareSpec.scala` | 3 | 3 examples |
| `blog-api/src/test/scala/su/wps/blog/endpoints/LivenessRoutesSpec.scala` | 3 | 1 example |

### Total: 23 files (12 new + 9 modified + 2 test files)

---

## Verification Checklist

After each batch:

- **Batch 1:** `docker compose up --build` starts all 4 services + postgres successfully. blog-admin accessible at `:8000/admin/`.
- **Batch 2:** Push to master triggers docker-publish workflow. Images appear in GHCR. Deploy workflow (manual trigger) deploys to server.
- **Batch 3:** `sbt test` passes (309 existing + 4 new = 313 total). Production logs are JSON when `JAVA_OPTS=-Dlogback.configurationFile=logback-prod.xml`. `curl -i /health/live` returns 200 with `X-Request-Id` header.
- **Batch 4:** `docker compose run db-backup` creates a gzipped SQL dump. Restore script recovers from backup.
