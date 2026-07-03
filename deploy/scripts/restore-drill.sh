#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
backup="${1:-}"
drill_database="personal_blog_restore_drill_$(date -u +%Y%m%d%H%M%S)"
compose=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")

if [[ -z "$backup" ]]; then
  echo "Usage: $0 <backup.sql.gz.enc>" >&2
  exit 1
fi

cleanup() {
  "${compose[@]}" exec -T mysql sh -c \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE IF EXISTS `'"$drill_database"'`"' \
    >/dev/null 2>&1 || true
}
trap cleanup EXIT

RESTORE_CONFIRM="$drill_database" ENV_FILE="$ENV_FILE" \
  "$ROOT_DIR/deploy/scripts/restore-mysql.sh" "$backup" "$drill_database"

result="$("${compose[@]}" exec -T mysql sh -c \
  'mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" "'"$drill_database"'" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE(); SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1;"')"
table_count="$(printf '%s\n' "$result" | sed -n '1p')"
migration_count="$(printf '%s\n' "$result" | sed -n '2p')"

if [[ "${table_count:-0}" -lt 10 || "${migration_count:-0}" -lt 1 ]]; then
  echo "Restore drill validation failed: tables=$table_count migrations=$migration_count" >&2
  exit 1
fi

echo "Restore drill passed: tables=$table_count migrations=$migration_count"
