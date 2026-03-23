# Repository Guidelines

## Project Structure & Module Organization
The project is a Spring Boot 3.5.x backend that serves as the transactional core for the `despesas` ecosystem. The official front-door is Flutter Web, mounted into the backend via `APP_FRONTEND_WEB_DIST`; Flutter Mobile is a companion client of the same API.

- `src/main/java/com/gilrossi/despesas/api/v1/`: versioned REST API controllers.
- `catalog/`, `expense/`, `identity/`, `payment/`: main business domains.
- `emailingestion/`: operational e-mail ingestion and review flows used by n8n.
- `financialassistant/`: deterministic analytics plus optional AI gateway.
- `security/`: Bearer auth, authorization and household isolation.
- `service/` and `model/`: legacy adapter layer kept only while the old MVC shell is being phased out.
- `src/main/resources/db/migration/`: Flyway migrations.
- `src/test/java/com/gilrossi/despesas/`: unit, slice and integration tests.
- `deploy/`: production compose and env templates.
- `n8n/workflows/`: mirror snapshot of the canonical workflows stored in the private n8n repository.

## Build, Test, and Development Commands
- `scripts/runtime/run-local-postgres.sh`: loads governed envs and starts local PostgreSQL.
- `scripts/runtime/run-local-backend.sh`: loads governed envs and runs the backend.
- `./mvnw test`: runs the main JUnit suite.
- `source scripts/runtime/load-governed-env.sh local && bash scripts/run-postgres-it.sh`: runs the PostgreSQL/Flyway integration suite with governed envs loaded.
- `scripts/runtime/validate-production-compose.sh`: renders and validates the production compose without deploying.

Access the app at `http://localhost:8080/`, the API at `http://localhost:8080/api/v1` and the healthcheck at `http://localhost:8080/actuator/health`.

## Coding Style & Naming Conventions
- Language/runtime: Java 21, Spring Boot 3.5.x.
- Follow the existing Java style: tab-indented classes and methods, clear Portuguese domain names where the repo already uses them.
- Class names: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Controllers should stay thin; domain services and use cases hold the business rules.
- Prefer constructor injection for Spring beans.

## Testing Guidelines
- Framework: JUnit 5 via `spring-boot-starter-test`.
- Mirror package structure under `src/test/java`.
- Test class names: `<ClassName>Test` or `<FlowName>IntegrationTest`.
- Prioritize API contracts, auth/authz, household isolation, e-mail ingestion and financial-assistant behavior before merging.

## Commit & Pull Request Guidelines
- Keep commit messages concise and action-oriented.
- One logical change per commit.
- PRs should include objective, affected layers, validation performed and screenshots only when UI-facing docs/assets change.
- Link related issue/ticket when available.

## Security & Configuration Tips
- Never commit real credentials, env files or decrypted runtime exports.
- Prefer governed envs under `~/envs/despesas/<env>/`.
- Review `.gitignore` before adding AI artifacts, prompts, runtime dumps or operational archives.
