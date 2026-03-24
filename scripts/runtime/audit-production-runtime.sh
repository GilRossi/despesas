#!/usr/bin/env bash
set -euo pipefail

VPS_HOST="${VPS_HOST:?VPS_HOST must be set}"
VPS_USER="${VPS_USER:-root}"
SSH_KEY_PATH="${SSH_KEY_PATH:-$HOME/.ssh/deploy_key}"
REMOTE_DEPLOY_DIR="${REMOTE_DEPLOY_DIR:-/opt/despesas/deploy}"
REMOTE_ENV_DIR="${REMOTE_ENV_DIR:-/root/envs/despesas/prod}"
SANITIZE_ZERO_BYTE_ENV_TMP="${SANITIZE_ZERO_BYTE_ENV_TMP:-false}"

ssh_cmd=(
	ssh
	-i "$SSH_KEY_PATH"
	-o BatchMode=yes
	-o StrictHostKeyChecking=yes
	"${VPS_USER}@${VPS_HOST}"
)

local_compose_base_hash="$(sha256sum deploy/compose.base.yml | awk '{print $1}')"
local_compose_prod_hash="$(sha256sum deploy/compose.prod.yml | awk '{print $1}')"

remote_compose_base_hash="$("${ssh_cmd[@]}" "sha256sum '$REMOTE_DEPLOY_DIR/compose.base.yml' | awk '{print \$1}'")"
remote_compose_prod_hash="$("${ssh_cmd[@]}" "sha256sum '$REMOTE_DEPLOY_DIR/compose.prod.yml' | awk '{print \$1}'")"

drift_detected="false"

compose_base_status="match"
if [ "$local_compose_base_hash" != "$remote_compose_base_hash" ]; then
	compose_base_status="drift"
	drift_detected="true"
fi

compose_prod_status="match"
if [ "$local_compose_prod_hash" != "$remote_compose_prod_hash" ]; then
	compose_prod_status="drift"
	drift_detected="true"
fi

containers_summary="$("${ssh_cmd[@]}" "docker ps --format '{{.Names}}|{{.Image}}|{{.Status}}' | sort")"
compose_labels_summary="$("${ssh_cmd[@]}" "docker inspect despesas-backend-1 despesas-postgres-1 despesas-n8n-1 --format '{{.Name}}|{{index .Config.Labels \"com.docker.compose.project\"}}|{{index .Config.Labels \"com.docker.compose.project.working_dir\"}}|{{index .Config.Labels \"com.docker.compose.project.config_files\"}}' | sort")"
env_files_summary="$("${ssh_cmd[@]}" "find '$REMOTE_ENV_DIR' -maxdepth 1 -type f -printf '%f|%s\n' | sort")"

tmp_file_size="$("${ssh_cmd[@]}" "if [ -e '$REMOTE_ENV_DIR/backend.env.tmp' ]; then stat -c '%s' '$REMOTE_ENV_DIR/backend.env.tmp'; else echo ABSENT; fi")"
tmp_file_action="kept"

if [ "$SANITIZE_ZERO_BYTE_ENV_TMP" = "true" ] && [ "$tmp_file_size" = "0" ]; then
	"${ssh_cmd[@]}" "rm -f '$REMOTE_ENV_DIR/backend.env.tmp'"
	tmp_file_action="removed"
fi

printf 'compose.base.yml|%s|%s\n' "$remote_compose_base_hash" "$compose_base_status"
printf 'compose.prod.yml|%s|%s\n' "$remote_compose_prod_hash" "$compose_prod_status"
printf 'backend.env.tmp|%s|%s\n' "$tmp_file_size" "$tmp_file_action"
printf '\n[containers]\n%s\n' "$containers_summary"
printf '\n[compose-labels]\n%s\n' "$compose_labels_summary"
printf '\n[env-files]\n%s\n' "$env_files_summary"

if [ "$drift_detected" = "true" ]; then
	echo "runtime-drift|detected" >&2
	exit 1
fi
