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
