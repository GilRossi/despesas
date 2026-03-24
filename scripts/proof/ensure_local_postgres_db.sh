#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

# shellcheck source=/dev/null
source "${repo_root}/scripts/runtime/load-governed-env.sh" local

cd "${repo_root}"
docker compose up -d postgres >/dev/null

for _ in {1..30}; do
  if docker exec despesas-postgres pg_isready -U "${DB_USERNAME}" -d postgres >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

database_exists="$(
  docker exec despesas-postgres psql -U "${DB_USERNAME}" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'"
)"

if [[ "${database_exists}" != "1" ]]; then
  docker exec despesas-postgres psql -U "${DB_USERNAME}" -d postgres -c "CREATE DATABASE ${DB_NAME}" >/dev/null
fi

echo "postgres_ready db=${DB_NAME}"
