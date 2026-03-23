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
traefik_container_name="$(printf '%s\n' "${traefik_containers}" | head -n 1 | cut -f 1)"

if [[ -n "${traefik_containers}" ]]; then
  echo "Traefik detectado no host:"
  echo "${traefik_containers}"
else
  echo "PENDENTE: nenhum container Traefik foi detectado no host."
fi

if [[ -n "${traefik_container_name}" ]]; then
  traefik_network_mode="$(docker inspect "${traefik_container_name}" --format '{{.HostConfig.NetworkMode}}')"
  traefik_cmd_json="$(docker inspect "${traefik_container_name}" --format '{{json .Config.Cmd}}')"

  if [[ "${traefik_network_mode}" == "host" ]]; then
    echo "OK Traefik em host network."
  else
    echo "PENDENTE Traefik em host network: modo atual ${traefik_network_mode}"
  fi

  TRAEFIK_CMD_JSON="${traefik_cmd_json}" TRAEFIK_CERTRESOLVER="${TRAEFIK_CERTRESOLVER:-letsencrypt}" python3 - <<'PY'
import json
import os

cmd = json.loads(os.environ["TRAEFIK_CMD_JSON"] or "[]")
certresolver = os.environ["TRAEFIK_CERTRESOLVER"]
checks = [
    ("provider Docker", "--providers.docker=true"),
    ("docker exposedByDefault=false", "--providers.docker.exposedbydefault=false"),
    ("entrypoint web=:80", "--entrypoints.web.address=:80"),
    ("entrypoint websecure=:443", "--entrypoints.websecure.address=:443"),
    (f"certresolver {certresolver}", f"--certificatesresolvers.{certresolver}.acme.httpchallenge=true"),
]

for label, expected_flag in checks:
    status = "OK" if expected_flag in cmd else "PENDENTE"
    print(f"{status} {label}")
PY
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

echo "Preflight do host concluido sem subir containers."
