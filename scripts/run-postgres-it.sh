#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

docker-compose up -d >/dev/null

DB_NAME="despesas_it_$(date +%s)"
docker exec despesas-postgres psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1 \
  || docker exec despesas-postgres createdb -U postgres "${DB_NAME}"

TEST_DB_URL="jdbc:postgresql://127.0.0.1:5432/${DB_NAME}" bash ./mvnw test \
  -Dtest=FlywayMigrationIT,LegacyExpenseBackfillReadIT,CategoryJpaRepositoryAdapterIT,SubcategoryJpaRepositoryAdapterIT,ExpenseRepositoryIT,PaymentRepositoryIT,ExpensePaymentPersistenceIT
