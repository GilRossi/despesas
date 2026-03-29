# MVP Test Matrix

## Scope
- Backend: `/home/gil/workspace/claude/despesas`
- Frontend: `/home/gil/StudioProjects/despesas_frontend`

## Coverage goals
- Preserve current green baseline on backend and frontend.
- Measure and track line coverage continuously.
- Keep the matrix focused on critical user journeys and regressions that block real use.

## Critical flows

| Flow | Backend coverage | Frontend coverage | Current risk |
| --- | --- | --- | --- |
| Authentication / session | Present via controller and integration tests | Present via screen and router tests | Medium |
| Onboarding / tour | Present via onboarding integration tests | Present via assistant screen tests | Medium |
| Dashboard OWNER | Present via dashboard controller/integration tests | Present via dashboard screen tests | High |
| Dashboard MEMBER | Partial, inferred through dashboard role behavior | Partial, mostly via shared UI tests | High |
| Create expense | Present via expense API/service tests | Present via form/router tests | High |
| Expense detail / edit / delete | Present via service/API tests | Present via detail screen tests | High |
| Expense payment | Present via payment service/API tests | Present via payment screen tests | High |
| Space references | Present via repository/service/controller tests | Present via screen/router tests | High |
| Fixed bills | Present via integration/controller tests | Present via form tests | High |
| History import | Present via integration/controller tests | Present via form tests | High |
| Reports | Present via reporting tests | Present via screen/view-model tests | Medium |
| Deep links and shell navigation | Limited backend value | Present via router tests | High |

## Regression focus
- No dead-end screens.
- Success states must lead to a usable next action.
- Created records must reappear in the correct list/detail surface.
- OWNER dashboard must not fail on summary loading.
- Tour must remain re-openable after completion.

## Baseline measurement commands
- Backend: `./mvnw test` then `./mvnw jacoco:report`
- Frontend: `flutter test --coverage`

## Remaining gaps
- Backend coverage is measurable and currently sits at `85.96%` line coverage, below the `100%` goal.
- Frontend coverage is measurable via `coverage/lcov.info` and currently sits at `83.92%` line coverage, below the `100%` goal.
- The matrix still needs explicit E2E coverage for deep links, browser back/forward, and post-submit continuity.
