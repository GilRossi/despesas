#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

# shellcheck source=/dev/null
source "${script_dir}/load-governed-env.sh" local

cd "${repo_root}"
exec ./mvnw spring-boot:run "$@"
