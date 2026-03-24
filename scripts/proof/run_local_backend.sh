#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

"${script_dir}/ensure_local_postgres_db.sh" >/dev/null

# shellcheck source=/dev/null
source "${repo_root}/scripts/runtime/load-governed-env.sh" local

export APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID="${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID:-macro3-local-key}"
export APP_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET:-macro3-local-secret}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:+${SPRING_PROFILES_ACTIVE},}local-proof"

cd "${repo_root}"
exec ./mvnw spring-boot:run
