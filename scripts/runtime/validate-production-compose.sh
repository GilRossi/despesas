#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

# shellcheck source=/dev/null
source "${script_dir}/load-governed-env.sh" prod

: "${BACKEND_IMAGE:=ghcr.io/example/despesas-backend:main}"
: "${APP_PUBLIC_HOST:=app.example.com}"
: "${N8N_PUBLIC_HOST:=n8n.example.com}"
: "${APP_SECURITY_CORS_ALLOWED_ORIGIN_PATTERNS:=https://${APP_PUBLIC_HOST}}"
: "${N8N_BLOCK_ENV_ACCESS_IN_NODE:=false}"
: "${TRAEFIK_ENTRYPOINTS:=websecure}"
: "${TRAEFIK_CERTRESOLVER:=letsencrypt}"
export BACKEND_IMAGE APP_PUBLIC_HOST N8N_PUBLIC_HOST APP_SECURITY_CORS_ALLOWED_ORIGIN_PATTERNS
export N8N_BLOCK_ENV_ACCESS_IN_NODE TRAEFIK_ENTRYPOINTS TRAEFIK_CERTRESOLVER

cd "${repo_root}"
docker compose \
  -f deploy/compose.base.yml \
  -f deploy/compose.prod.yml \
  config >/tmp/despesas-production-compose.rendered.yml

echo "Compose de producao validado em /tmp/despesas-production-compose.rendered.yml"
