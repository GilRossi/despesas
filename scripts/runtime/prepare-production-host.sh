#!/usr/bin/env bash
set -euo pipefail

despesas_root="${DESPESAS_ROOT:-/srv/despesas}"
env_root="${DESPESAS_ENV_ROOT:-$HOME/envs/despesas}"
runtime_env="${1:-prod}"

if [[ "${runtime_env}" != "prod" ]]; then
  echo "Este script prepara apenas o host de producao." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker nao encontrado no host." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose nao encontrado no host." >&2
  exit 1
fi

traefik_containers="$(docker ps --format '{{.Names}}\t{{.Image}}' | grep -i traefik || true)"

if [[ -n "${traefik_containers}" ]]; then
  echo "Traefik detectado no host:"
  echo "${traefik_containers}"
else
  echo "PENDENTE: nenhum container Traefik foi detectado no host."
fi

mkdir -p "${despesas_root}/frontend-web/current"
mkdir -p "${despesas_root}/n8n/files"
mkdir -p "${env_root}/${runtime_env}"

chmod 700 "${env_root}" "${env_root}/${runtime_env}" || true

echo "Diretorios preparados:"
echo "- ${despesas_root}/frontend-web/current"
echo "- ${despesas_root}/n8n/files"
echo "- ${env_root}/${runtime_env}"

for file_name in backend.env postgres.env n8n.env google.env microsoft.env; do
  file_path="${env_root}/${runtime_env}/${file_name}"
  if [[ -f "${file_path}" ]]; then
    chmod 600 "${file_path}" || true
    echo "OK env: ${file_path}"
  else
    echo "PENDENTE env: ${file_path}"
  fi
done

if [[ -n "${TRAEFIK_PUBLIC_NETWORK:-}" ]]; then
  if docker network inspect "${TRAEFIK_PUBLIC_NETWORK}" >/dev/null 2>&1; then
    echo "OK rede Traefik externa: ${TRAEFIK_PUBLIC_NETWORK}"
  else
    echo "PENDENTE rede Traefik externa: ${TRAEFIK_PUBLIC_NETWORK}"
  fi
else
  echo "PENDENTE rede Traefik externa: exporte TRAEFIK_PUBLIC_NETWORK antes da primeira subida."
fi

echo "Preflight do host concluido sem subir containers."
