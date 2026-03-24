#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
frontend_repo="${FRONTEND_REPO_ROOT:-/home/gil/StudioProjects/despesas_frontend}"
n8n_repo="${N8N_REPO_ROOT:-/home/gil/n8n-local}"
artifacts_dir="${repo_root}/build/local_e2e"
summary_file="${artifacts_dir}/proof-summary.json"
backend_log="${artifacts_dir}/backend.log"

mkdir -p "${artifacts_dir}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command curl
require_command jq
require_command python3

fail_with_body_args=()
if curl --help all 2>/dev/null | grep -q -- '--fail-with-body'; then
  fail_with_body_args+=(--fail-with-body)
else
  fail_with_body_args+=(--fail)
fi

wait_for_http() {
  local url="$1"
  local timeout="${2:-60}"
  local started_at
  started_at="$(date +%s)"
  while true; do
    if curl -fsS "${url}" >/dev/null 2>&1 || curl -s -o /dev/null "${url}" >/dev/null 2>&1; then
      return 0
    fi
    if (( "$(date +%s)" - started_at >= timeout )); then
      echo "Timed out waiting for ${url}" >&2
      return 1
    fi
    sleep 1
  done
}

api_post() {
  local url="$1"
  local token="$2"
  local body="$3"
  curl "${fail_with_body_args[@]}" -sS -X POST "${url}" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${token}" \
    -d "${body}"
}

api_get() {
  local url="$1"
  local token="$2"
  curl "${fail_with_body_args[@]}" -sS "${url}" -H "Authorization: Bearer ${token}"
}

login_and_capture() {
  local email="$1"
  local password="$2"
  local response
  response="$(
    curl "${fail_with_body_args[@]}" -sS -X POST http://localhost:8080/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d "$(jq -nc --arg email "${email}" --arg password "${password}" '{email:$email,password:$password}')"
  )"
  printf '%s' "${response}"
}

preflight_login_status() {
  curl -s -o /tmp/proof-login-preflight.out -w '%{http_code}' \
    -X OPTIONS http://localhost:8080/api/v1/auth/login \
    -H 'Origin: http://localhost:54721' \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type'
}

ensure_backend() {
  "${script_dir}/ensure_local_postgres_db.sh" >/dev/null
  if curl -s -o /dev/null http://localhost:8080/api/v1/auth/me; then
    if [[ "$(preflight_login_status)" == "200" ]]; then
      return 0
    fi
    echo "Backend already running on localhost:8080 without local-proof CORS support. Stop it and rerun the smoke." >&2
    return 1
  fi

  (
    cd "${repo_root}"
    nohup "${script_dir}/run_local_backend.sh" \
      >"${backend_log}" 2>&1 &
    echo $! > "${artifacts_dir}/backend.pid"
  )

  wait_for_http "http://localhost:8080/api/v1/auth/me" 90
  if [[ "$(preflight_login_status)" != "200" ]]; then
    echo "Local-proof backend started without answering the required CORS preflight." >&2
    return 1
  fi
}

ensure_n8n() {
  (
    cd "${n8n_repo}"
    # shellcheck source=/dev/null
    source scripts/load_governed_env.sh local >/dev/null
    export DESPESAS_OPERATIONAL_EMAIL_INGESTION_KEY_ID="${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID:-macro3-local-key}"
    export DESPESAS_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET:-macro3-local-secret}"
    scripts/run_local.sh >/dev/null
    scripts/proof/ensure_email_ingestion_webhooks.sh >/dev/null
  )
  wait_for_http "http://localhost:5678/" 60
}

write_summary() {
  local payload="$1"
  printf '%s\n' "${payload}" > "${summary_file}"
}

# shellcheck source=/dev/null
source "${repo_root}/scripts/runtime/load-governed-env.sh" local

export APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID="${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID:-macro3-local-key}"
export APP_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET:-macro3-local-secret}"

proof_run_id="${PROOF_RUN_ID:-$(date +%s)}"
owner_email="owner-proof-${proof_run_id}@local.invalid"
member_email="member-proof-${proof_run_id}@local.invalid"
owner_b_email="owner-b-proof-${proof_run_id}@local.invalid"
owner_password="${PROOF_OWNER_PASSWORD:-senha123}"
member_password="${PROOF_MEMBER_PASSWORD:-senha456}"
source_account="financeiro+${proof_run_id}@gmail.com"
proof_expense_b="Despesa household B ${proof_run_id}"

ensure_backend
ensure_n8n

admin_login="$(login_and_capture "${APP_BOOTSTRAP_PLATFORM_ADMIN_EMAIL}" "${APP_BOOTSTRAP_PLATFORM_ADMIN_PASSWORD}")"
admin_token="$(printf '%s' "${admin_login}" | jq -r '.data.accessToken')"

admin_household_create="$(
  api_post \
    "http://localhost:8080/api/v1/admin/households" \
    "${admin_token}" \
    "$(jq -nc --arg household "Household Prova ${proof_run_id}" --arg name 'Owner Prova' --arg email "${owner_email}" --arg password "${owner_password}" '{householdName:$household,ownerName:$name,ownerEmail:$email,ownerPassword:$password}')"
)"

export API_BASE_URL="http://localhost:8080"
export PROOF_RUN_ID="${proof_run_id}"
export PROOF_BROWSER_MODE="${PROOF_BROWSER_MODE:-headed}"
export PROOF_OWNER_EMAIL="${owner_email}"
export PROOF_OWNER_PASSWORD="${owner_password}"
export PROOF_MEMBER_EMAIL="${member_email}"
export PROOF_MEMBER_PASSWORD="${member_password}"

(
  cd "${frontend_repo}"
  scripts/run_web_e2e_proof.sh >/dev/null
)

owner_login="$(login_and_capture "${owner_email}" "${owner_password}")"
member_login="$(login_and_capture "${member_email}" "${member_password}")"
owner_token="$(printf '%s' "${owner_login}" | jq -r '.data.accessToken')"
member_token="$(printf '%s' "${member_login}" | jq -r '.data.accessToken')"

owner_admin_attempt_status="$(
  curl -s -o /tmp/proof-owner-admin.json -w '%{http_code}' \
    -X POST http://localhost:8080/api/v1/admin/households \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${owner_token}" \
    -d "$(jq -nc --arg household "Forbidden ${proof_run_id}" --arg name 'Owner x' --arg email "forbidden-${proof_run_id}@local.invalid" --arg password 'senha123' '{householdName:$household,ownerName:$name,ownerEmail:$email,ownerPassword:$password}')"
)"

member_create_attempt_status="$(
  curl -s -o /tmp/proof-member-create.json -w '%{http_code}' \
    -X POST http://localhost:8080/api/v1/household/members \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${member_token}" \
    -d "$(jq -nc --arg name 'Blocked Member' --arg email "blocked-${proof_run_id}@local.invalid" --arg password 'senha123' '{name:$name,email:$email,password:$password}')"
)"

source_create_response="$(
  api_post \
    "http://localhost:8080/api/v1/email-ingestion/sources" \
    "${owner_token}" \
    "$(jq -nc --arg sourceAccount "${source_account}" --arg label "Source ${proof_run_id}" '{sourceAccount:$sourceAccount,label:$label}')"
)"

catalog_options="$(api_get "http://localhost:8080/api/v1/catalog/options" "${owner_token}")"
category_id="$(printf '%s' "${catalog_options}" | jq -r '.data[0].id')"
subcategory_id="$(printf '%s' "${catalog_options}" | jq -r '.data[0].subcategories[0].id')"

owner_b_create="$(
  api_post \
    "http://localhost:8080/api/v1/admin/households" \
    "${admin_token}" \
    "$(jq -nc --arg household "Household B ${proof_run_id}" --arg name 'Owner B Proof' --arg email "${owner_b_email}" --arg password "${owner_password}" '{householdName:$household,ownerName:$name,ownerEmail:$email,ownerPassword:$password}')"
)"

owner_b_login="$(login_and_capture "${owner_b_email}" "${owner_password}")"
owner_b_token="$(printf '%s' "${owner_b_login}" | jq -r '.data.accessToken')"
owner_b_catalog_options="$(api_get "http://localhost:8080/api/v1/catalog/options" "${owner_b_token}")"
owner_b_category_id="$(printf '%s' "${owner_b_catalog_options}" | jq -r '.data[0].id')"
owner_b_subcategory_id="$(printf '%s' "${owner_b_catalog_options}" | jq -r '.data[0].subcategories[0].id')"
owner_b_expense_create="$(
api_post \
  "http://localhost:8080/api/v1/expenses" \
  "${owner_b_token}" \
  "$(jq -nc --arg description "${proof_expense_b}" --argjson categoryId "${owner_b_category_id}" --argjson subcategoryId "${owner_b_subcategory_id}" '{description:$description,amount:987.65,dueDate:"2026-03-25",context:"GERAL",categoryId:$categoryId,subcategoryId:$subcategoryId,notes:"Cross-household proof"}')"
)"
owner_b_expense_id="$(printf '%s' "${owner_b_expense_create}" | jq -er '.data.id')"
owner_b_expense_detail="$(
  api_get "http://localhost:8080/api/v1/expenses/${owner_b_expense_id}" "${owner_b_token}"
)"
owner_a_cross_household_status="$(
  curl -s -o /tmp/proof-owner-a-cross-household.json -w '%{http_code}' \
    "http://localhost:8080/api/v1/expenses/${owner_b_expense_id}" \
    -H "Authorization: Bearer ${owner_token}"
)"

assistant_response="$(
  api_post \
    "http://localhost:8080/api/v1/financial-assistant/query" \
    "${owner_token}" \
    '{"question":"Onde estou gastando mais neste mes?","referenceMonth":"2026-03"}'
)"

reports_response="$(api_get "http://localhost:8080/api/v1/dashboard/summary" "${owner_token}")"
owner_expenses="$(api_get "http://localhost:8080/api/v1/expenses?page=0&size=20" "${owner_token}")"
owner_b_expenses="$(api_get "http://localhost:8080/api/v1/expenses?page=0&size=20" "${owner_b_token}")"

manual_replay_response="$(
  curl "${fail_with_body_args[@]}" -sS -X POST http://localhost:5678/webhook/email-ingestion-replay-v1 \
    -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg sourceAccount "${source_account}" --arg externalMessageId "manual-${proof_run_id}" --arg backendBaseUrl "http://host.docker.internal:8080" '{scenario:"manualPurchase",sourceAccount:$sourceAccount,externalMessageId:$externalMessageId,backendBaseUrl:$backendBaseUrl}')"
)"

recurring_replay_response="$(
  curl "${fail_with_body_args[@]}" -sS -X POST http://localhost:5678/webhook/email-ingestion-replay-v1 \
    -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg sourceAccount "${source_account}" --arg externalMessageId "recurring-${proof_run_id}" --arg backendBaseUrl "http://host.docker.internal:8080" '{scenario:"recurringBill",sourceAccount:$sourceAccount,externalMessageId:$externalMessageId,backendBaseUrl:$backendBaseUrl}')"
)"

reviews_response="$(api_get "http://localhost:8080/api/v1/email-ingestion/reviews?page=0&size=20" "${owner_token}")"

operational_valid_body_file="${artifacts_dir}/operational-valid.json"
operational_forbidden_body_file="${artifacts_dir}/operational-forbidden.json"

cat > "${operational_valid_body_file}" <<EOF
{"sourceAccount":"${source_account}","externalMessageId":"signed-${proof_run_id}","sender":"fatura@provedor.com.br","subject":"Fatura Internet signed proof","receivedAt":"2026-03-23T21:00:00Z","merchantOrPayee":"Internet","suggestedCategoryName":"Casa","suggestedSubcategoryName":"Internet","totalAmount":129.9,"dueDate":"2026-03-25","occurredOn":"2026-03-23","currency":"BRL","items":[],"summary":"Signed operational proof","classification":"RECURRING_BILL","confidence":0.97,"rawReference":"signed-${proof_run_id}","desiredDecision":"AUTO_IMPORT"}
EOF

cat > "${operational_forbidden_body_file}" <<EOF
{"householdId":999,"sourceAccount":"${source_account}","externalMessageId":"forbidden-${proof_run_id}","sender":"manual@local.invalid","subject":"Forbidden household proof","receivedAt":"2026-03-23T21:10:00Z","merchantOrPayee":"Manual","suggestedCategoryName":"Casa","suggestedSubcategoryName":"Internet","totalAmount":55.0,"dueDate":"2026-03-26","occurredOn":"2026-03-23","currency":"BRL","items":[],"summary":"Forbidden household field proof","classification":"MANUAL_PURCHASE","confidence":0.8,"rawReference":"forbidden-${proof_run_id}","desiredDecision":"REVIEW"}
EOF

fixed_timestamp="$(date +%s)"
fixed_nonce="replay-${proof_run_id}"
operational_first="$(
  APP_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET}" \
  python3 "${script_dir}/post_signed_operational_request.py" \
    --url http://localhost:8080/api/v1/operations/email-ingestions \
    --key-id "${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID}" \
    --secret-env APP_OPERATIONAL_EMAIL_INGESTION_SECRET \
    --timestamp "${fixed_timestamp}" \
    --nonce "${fixed_nonce}" \
    --body-file "${operational_valid_body_file}"
)"
operational_replay="$(
  APP_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET}" \
  python3 "${script_dir}/post_signed_operational_request.py" \
    --url http://localhost:8080/api/v1/operations/email-ingestions \
    --key-id "${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID}" \
    --secret-env APP_OPERATIONAL_EMAIL_INGESTION_SECRET \
    --timestamp "${fixed_timestamp}" \
    --nonce "${fixed_nonce}" \
    --body-file "${operational_valid_body_file}"
)"
operational_forbidden="$(
  APP_OPERATIONAL_EMAIL_INGESTION_SECRET="${APP_OPERATIONAL_EMAIL_INGESTION_SECRET}" \
  python3 "${script_dir}/post_signed_operational_request.py" \
    --url http://localhost:8080/api/v1/operations/email-ingestions \
    --key-id "${APP_OPERATIONAL_EMAIL_INGESTION_KEY_ID}" \
    --secret-env APP_OPERATIONAL_EMAIL_INGESTION_SECRET \
    --body-file "${operational_forbidden_body_file}"
)"

audit_events_json="$(
  docker exec despesas-postgres psql -U "${DB_USERNAME}" -d "${DB_NAME}" -At -F $'\t' -c \
    "select event_type, detail_code, primary_reference, source_key from persisted_audit_events order by id desc limit 25"
)"

assistant_mentions_b="false"
if printf '%s' "${assistant_response}" | grep -q "${proof_expense_b}"; then
  assistant_mentions_b="true"
fi

owner_b_has_proof="$(
  printf '%s' "${owner_b_expenses}" | jq -r --arg description "${proof_expense_b}" '[((.data.items // .items // [])[]?) | select(.description == $description)] | length'
)"
owner_a_has_b_expense="$(
  printf '%s' "${owner_expenses}" | jq -r --arg description "${proof_expense_b}" '[((.data.items // .items // [])[]?) | select(.description == $description)] | length'
)"
owner_b_detail_description_matches="$(
  printf '%s' "${owner_b_expense_detail}" | jq -r --arg description "${proof_expense_b}" '.data.description == $description'
)"

write_summary "$(
  jq -n \
    --arg runId "${proof_run_id}" \
    --arg screenshotsDir "${frontend_repo}/build/local_e2e/screenshots" \
    --arg backendLog "${backend_log}" \
    --arg ownerEmail "${owner_email}" \
    --arg memberEmail "${member_email}" \
    --arg sourceAccount "${source_account}" \
    --arg ownerAdminAttemptStatus "${owner_admin_attempt_status}" \
    --arg memberCreateAttemptStatus "${member_create_attempt_status}" \
    --arg ownerACrossHouseholdStatus "${owner_a_cross_household_status}" \
    --arg assistantMentionsB "${assistant_mentions_b}" \
    --arg ownerAHasBExpense "${owner_a_has_b_expense}" \
    --arg ownerBHasProof "${owner_b_has_proof}" \
    --arg ownerBDetailDescriptionMatches "${owner_b_detail_description_matches}" \
    --argjson adminProvisioning "${admin_household_create}" \
    --argjson ownerBExpenseCreate "${owner_b_expense_create}" \
    --argjson ownerBExpenseDetail "${owner_b_expense_detail}" \
    --argjson reports "${reports_response}" \
    --argjson assistant "${assistant_response}" \
    --argjson recurringReplay "${recurring_replay_response}" \
    --argjson manualReplay "${manual_replay_response}" \
    --argjson reviews "${reviews_response}" \
    --argjson operationalFirst "${operational_first}" \
    --argjson operationalReplay "${operational_replay}" \
    --argjson operationalForbidden "${operational_forbidden}" \
    --arg auditEvents "${audit_events_json}" \
    '{
      runId: $runId,
      screenshotsDir: $screenshotsDir,
      backendLog: $backendLog,
      ownerEmail: $ownerEmail,
      memberEmail: $memberEmail,
      sourceAccount: $sourceAccount,
      adminProvisioning: $adminProvisioning,
      accessControl: {
        ownerAdminAttemptStatus: ($ownerAdminAttemptStatus | tonumber),
        memberCreateAttemptStatus: ($memberCreateAttemptStatus | tonumber)
      },
      assistantIsolation: {
        ownerBExpenseId: ($ownerBExpenseCreate.data.id),
        ownerBExpenseDetailStatus: 200,
        ownerBExpenseDetailDescriptionMatches: ($ownerBDetailDescriptionMatches == "true"),
        ownerACrossHouseholdDetailStatus: ($ownerACrossHouseholdStatus | tonumber),
        mentionsHouseholdBExpense: ($assistantMentionsB == "true"),
        ownerAHasHouseholdBExpense: ($ownerAHasBExpense | tonumber),
        ownerBHasOwnProofExpense: ($ownerBHasProof | tonumber),
        ownerBExpenseDetail: $ownerBExpenseDetail,
        response: $assistant
      },
      dashboard: $reports,
      n8n: {
        recurringReplay: $recurringReplay,
        manualReplay: $manualReplay,
        reviews: $reviews
      },
      operationalSigned: {
        first: $operationalFirst,
        replay: $operationalReplay,
        forbiddenHousehold: $operationalForbidden
      },
      auditEventsPreview: ($auditEvents | split("\n") | map(select(length > 0)))
    }'
)"

cat "${summary_file}"
