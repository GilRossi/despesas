# MVP Coverage Summary

## Current snapshot

### Backend
- Coverage instrumentation: JaCoCo via Maven.
- Report location: `target/site/jacoco/index.html`
- Current measurement:
  - branch coverage: `702/1229` (`57.12%`)
  - line coverage: `4482/5214` (`85.96%`)
- Current enforcement: report generation only, no fail gate yet.

### Frontend
- Coverage artifact present: `coverage/lcov.info`
- Current measurement:
  - line coverage: `5355/6381` (`83.92%`)

## What is already well covered
- Backend API and integration tests are broad across auth, dashboard, expenses, payments, onboarding, reports, assistant, imports, and references.
- Frontend has route, widget, repository, and screen tests across the main MVP surfaces.
- The latest stabilization pass left `flutter test`, `flutter analyze`, `flutter build web`, `./mvnw test`, and `./mvnw -DskipTests package` green.

## What is still missing
- A single machine-readable threshold that blocks regressions below the target.
- End-to-end coverage for deep links, browser back/forward, and refresh on internal routes.
- Explicit coverage accounting for every critical flow on both shells.
- The hard requirement of `100%` coverage is still unmet in both repos.

## Tooling changes made in this round
- Backend JaCoCo plugin added to Maven.
- QA documentation scaffold added under `docs/qa/`.

## Interpretation
- The current stabilized tree is measurably healthier than the previous baseline, but it is still below the `100%` threshold required by BLOCO 10.
- The remaining blocker is no longer “missing measurement”; it is the real uncovered surface that still exists in backend and frontend.
