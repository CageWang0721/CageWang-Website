#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
version="${1:-${APP_VERSION:-}}"

fail() {
  echo "Preflight failed: $*" >&2
  exit 1
}

read_env() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1 | tr -d '\r'
}

[[ -f "$ENV_FILE" ]] || fail "missing environment file: $ENV_FILE"
[[ "$version" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}\.[0-9]+$ ]] \
  || fail "version must use YYYY.MM.DD.N format"

if grep -q $'\r' "$ENV_FILE"; then
  fail "$ENV_FILE contains CRLF line endings; convert it to Linux LF before deployment"
fi

for command_name in docker openssl gzip; do
  command -v "$command_name" >/dev/null || fail "missing command: $command_name"
done
docker compose version >/dev/null || fail "Docker Compose v2 is unavailable"

required_keys=(
  MYSQL_DATABASE
  MYSQL_USERNAME
  MYSQL_PASSWORD
  MYSQL_ROOT_PASSWORD
  REDIS_PASSWORD
  JWT_SECRET
  PUBLIC_HOST
  PUBLIC_WWW_HOST
  ADMIN_HOST
  BACKUP_DIR
  BACKUP_ENCRYPTION_PASSWORD
)

for key in "${required_keys[@]}"; do
  value="$(read_env "$key")"
  [[ -n "$value" ]] || fail "$key is missing or blank in $ENV_FILE"
  normalized="${value,,}"
  case "$normalized" in
    replace-*|replace_*|change-me*|changeme*|example*|local-development*)
      fail "$key still contains an example or development placeholder"
      ;;
  esac
done

jwt_secret="$(read_env JWT_SECRET)"
if (( $(printf '%s' "$jwt_secret" | wc -c) < 32 )); then
  fail "JWT_SECRET must contain at least 32 UTF-8 bytes"
fi

github_enabled="$(read_env GITHUB_ENABLED)"
if [[ "${github_enabled,,}" =~ ^(1|true|yes|on)$ ]]; then
  [[ -n "$(read_env GITHUB_USERNAME)" ]] || fail "GITHUB_USERNAME is required when GitHub sync is enabled"
  [[ -n "$(read_env GITHUB_TOKEN)" ]] || fail "GITHUB_TOKEN is required when GitHub sync is enabled"
fi

backup_dir="$(read_env BACKUP_DIR)"
[[ "$backup_dir" == /* ]] || fail "BACKUP_DIR must be an absolute path outside the repository"
[[ "$backup_dir" != "$ROOT_DIR" && "$backup_dir" != "$ROOT_DIR/"* ]] \
  || fail "BACKUP_DIR must be outside $ROOT_DIR"
mkdir -p "$backup_dir" || fail "cannot create BACKUP_DIR: $backup_dir"
[[ -w "$backup_dir" ]] || fail "BACKUP_DIR is not writable: $backup_dir"

required_certificates=(
  public-fullchain.pem
  public-privkey.pem
  admin-fullchain.pem
  admin-privkey.pem
)
for certificate in "${required_certificates[@]}"; do
  [[ -s "$ROOT_DIR/deploy/certs/$certificate" ]] \
    || fail "missing or empty certificate: deploy/certs/$certificate"
done

export APP_VERSION="$version"
docker compose \
  --env-file "$ENV_FILE" \
  -f "$ROOT_DIR/docker-compose.yml" \
  -f "$ROOT_DIR/docker-compose.prod.yml" \
  config --quiet

echo "Production preflight passed for version $version."
