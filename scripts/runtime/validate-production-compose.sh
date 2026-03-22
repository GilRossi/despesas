#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

# shellcheck source=/dev/null
source "${script_dir}/load-governed-env.sh" prod

: "${BACKEND_IMAGE:=ghcr.io/example/despesas-backend:main}"
: "${APP_PUBLIC_HOST:=app.example.com}"
: "${N8N_PUBLIC_HOST:=n8n.example.com}"
: "${ACME_EMAIL:=ops@example.com}"

cd "${repo_root}"
docker compose \
  -f deploy/compose.base.yml \
  -f deploy/compose.prod.yml \
  config >/tmp/despesas-production-compose.rendered.yml

echo "Compose de producao validado em /tmp/despesas-production-compose.rendered.yml"
