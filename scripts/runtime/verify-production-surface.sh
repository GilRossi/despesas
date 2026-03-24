#!/usr/bin/env bash
set -euo pipefail

APP_BASE_URL="${APP_BASE_URL:-https://app.rossicompany.com.br}"
N8N_BASE_URL="${N8N_BASE_URL:-https://n8n.rossicompany.com.br}"

APP_BASE_URL="${APP_BASE_URL%/}"
N8N_BASE_URL="${N8N_BASE_URL%/}"

check_status() {
	local label="$1"
	local expected_status="$2"
	local url="$3"
	local actual_status

	actual_status="$(curl -sk -o /dev/null -w '%{http_code}' "$url")"
	printf '%s|%s|%s\n' "$label" "$url" "$actual_status"

	if [ "$actual_status" != "$expected_status" ]; then
		echo "Expected HTTP $expected_status for $label but got $actual_status" >&2
		return 1
	fi
}

check_status "backend-health" "200" "$APP_BASE_URL/actuator/health"
check_status "app-root" "200" "$APP_BASE_URL/"
check_status "password-console-removed" "404" "$APP_BASE_URL/password-console.html"
check_status "n8n-root" "200" "$N8N_BASE_URL/"

echo "production-surface|ok"
