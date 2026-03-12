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
