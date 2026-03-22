#!/usr/bin/env bash
set -euo pipefail

despesas_env_root="${DESPESAS_ENV_ROOT:-$HOME/envs/despesas}"
despesas_env_name="${1:-local}"
despesas_env_dir="${despesas_env_root}/${despesas_env_name}"

if [[ ! -d "${despesas_env_dir}" ]]; then
  echo "Diretorio de env nao encontrado: ${despesas_env_dir}" >&2
  return 1 2>/dev/null || exit 1
fi

load_env_file() {
  local file_path="$1"
  if [[ ! -f "${file_path}" ]]; then
    echo "Arquivo de env nao encontrado: ${file_path}" >&2
    return 1
  fi

  set -a
  # shellcheck disable=SC1090
  source "${file_path}"
  set +a
}

load_env_file "${despesas_env_dir}/postgres.env"
load_env_file "${despesas_env_dir}/backend.env"

if [[ -f "${despesas_env_dir}/google.env" ]]; then
  load_env_file "${despesas_env_dir}/google.env"
fi

if [[ -f "${despesas_env_dir}/microsoft.env" ]]; then
  load_env_file "${despesas_env_dir}/microsoft.env"
fi

if [[ -f "${despesas_env_dir}/n8n.env" ]]; then
  load_env_file "${despesas_env_dir}/n8n.env"
fi
