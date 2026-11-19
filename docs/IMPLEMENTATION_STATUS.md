# Implementation Status

Last reviewed: 2026-11-20

## Current state

Status: Phase 0, Phase 1.1, Phase 1.2, Phase 1.3 hardening, Phase 1.4 historical API completeness, Phase 1.5 historical race frontend, Phase 1.6 synthetic historical demo dataset and controlled fixture refresh, and Phase 2.1 news ingestion foundation implemented.

The approved Java-first modular monolith, Python AI service, React frontend, Flyway migrations, Docker Compose configuration, and CI workflow are present. Phase 1.3 adds deterministic fixture-based historical race foundations: season affiliations, rider entries, sessions, classifications, laps, race positions, provenance, and read-only Java APIs. Phase 1.5 adds a responsive frontend journey over those APIs. Phase 1.6 expands the same fixture-provider ingestion flow into a multi-season synthetic demo archive. No sectors, tyres, weather, standings, telemetry, news ingestion, ML models, RAG, agents, simulation, live timing, or external provider integration has been implemented.

The requested `docs/PHASE_0_EXECUTION_PLAN.md` filename does not exist in the approved documentation set. The equivalent plan is `docs/DEVELOPMENT_PLAN.md`, which remains the source used for Phase 0 sequencing.

## Implemented components

### Java

- Maven Spring Boot 3.5.7 project targeting Java 21.
- Approved package boundaries for identity, race, rider, team, circuit, analytics, ingestion, news, prediction, AI, simulation, and live modules.
- Actuator health/readiness endpoints.
- `/api/v1/system/status`.
- `/api/v1/system/integration/python` authenticated Java-to-Python health gateway.
- Validation example endpoint and centralized validation/unexpected-error envelopes.
- Request correlation IDs using `X-Request-Id`.
- Stateless security configuration with future API routes denied by default.
- PostgreSQL, Redis, JPA, Flyway, and internal HTTP client configuration.
- Historical provider adapter contract and provider registry.
- Fixture historical provider with normalized external references for contract testing.
- Identity mapping service/store with internal-entity validation, source-record provenance, idempotency, and conflict rejection.
- Typed normalized reference records and a dependency-ordered transactional ingestion service.
- JDBC persistence for all six reference entities with natural-key conflict detection and source provenance attachment.
- Reference ingestion job/attempt bookkeeping using the existing operational tables.
- Historical fixture records for affiliations, sessions, classifications, laps, and race-position snapshots.
- Flyway V3 race tables and provenance columns for affiliation and race records.
- Transactional race persistence with idempotent natural keys and explicit conflict rejection.
- Independent failed-attempt recording for race ingestion.
- Typed logical-key validation for duplicate session results, laps, and race-position snapshots.
- Immutable source-version conflict detection shared by reference and race ingestion.
- Atomic PostgreSQL source-record upsert with concurrent same-version convergence and conflicting-version rejection.
- PostgreSQL integration coverage proving failed race persistence rolls back while the FAILED attempt remains committed.
- Read-only historical APIs for seasons, events, sessions, results, laps, positions, and session-scoped rider comparison, with limit/offset metadata for all historical list endpoints.
- Expanded deterministic synthetic fixture archive: two seasons, two manufacturers, four teams, ten riders, two circuits, four events, twelve sessions, classifications for every session, ten-lap race records, and position progression.
- Explicit fixture refresh service that reconciles only `fixture-historical`-owned race, relationship, and obsolete reference records before reusing the normal ingestion services.
- Disabled-by-default `fixture-ingestion` profile with `motogp.ingestion.run-fixture=true` one-shot runner; normal startup never triggers ingestion.
- Java news module with configurable RSS/Atom adapter registry, XML-safe normalization, canonical URL/fingerprint deduplication, source health, ingestion attempts, and permitted metadata/excerpt persistence.
- Disabled-by-default `news-ingestion` profile with `motogp.news.run=true` one-shot runner and opt-in scheduler; no production feed URLs are configured.

### Python

- FastAPI application with typed Pydantic health contract.
- Public `/health` endpoint.
- Authenticated `/internal/v1/health` endpoint.
- Bearer-token internal service authentication.
- Isolated Python project configuration and pytest tests.

### Frontend

- React + TypeScript + Vite application.
- React Router and TanStack Query.
- Backend API client.
- Responsive historical race exploration: seasons, events, circuit/event details, sessions, classifications, rider lap timing, position progression, and same-session rider comparison.
- React Query API client using only the existing `/api` proxy and Java response envelopes.
- Explicit loading, empty, unavailable, error, and refresh states with persisted-data and fixture/demo labelling.
- Accessible controls, responsive tables/cards, and a lightweight SVG position chart without adding chart dependencies.
- No hardcoded MotoGP entities or race statistics.

### Infrastructure

- PostgreSQL/pgvector and Redis Compose services with health checks.
- Frontend Compose healthcheck and nginx-to-Java API proxy.
- Java, Python, and frontend container definitions.
- Frontend Docker builds use `node:22-slim` for the Rollup build stage and `npm ci` for lockfile-reproducible installation; nginx remains the runtime image.
- `.env.example` with no real secrets.
- GitHub Actions for Java, Python, frontend, and documentation checks.
- Foundational migration `V1__foundation.sql` containing provider, ingestion, raw payload, provenance, reference, affiliation, and session tables.
- Incremental migration `V2__fixture_historical_provider.sql` registering the fixture provider without changing the approved schema.

## Verification executed

| Check | Result |
|---|---|
| `mvn -B clean verify` | Passed: 28 tests, 0 failures, 0 errors; 7 Compose-gated tests skipped by default |
| Final `mvn -B clean verify` for Phase 1.4 pagination hardening | Passed: 42 tests, 0 failures, 0 errors; 8 Compose-gated tests skipped by default |
| Fixture ingestion unit tests | Passed: 5 race-ingestion tests, including all three typed logical duplicate-key cases; 5 reference-ingestion tests; 5 identity-mapping tests |
| Nullable lap timestamp API test | Passed: HTTP 200 response with JSON `null` for `observedAt` |
| Compose PostgreSQL ingestion integration tests | Passed explicitly with `runComposeIntegrationTests=true`: 7 tests, 0 failures/errors; includes rollback with FAILED attempt, source-version immutability/null behavior, concurrent same-version convergence, conflicting-version rejection, and reference/race idempotency |
| Compose PostgreSQL historical read-store integration test | Passed explicitly with `runComposeIntegrationTests=true`: 1 test, 0 failures/errors; verified season/event/session hierarchy, classification retrieval, stable lap/position ordering, nullable values, comparison query, and lap/position pagination query boundaries on PostgreSQL 16.16 |
| `python -m pytest` in isolated `.venv` | Passed: 3 tests, 0 failures |
| `npm run build` | Passed: TypeScript compilation and Vite production build |
| `npm test` | Passed: TypeScript check |
| Clean frontend `npm ci && npm run build && npm test` | Passed with locked versions: React Query 5.108.0, Vite 6.4.6, Rollup 4.68.6 |
| Frontend Docker image build | Passed: `motogp-intelligence-web:phase0-fix` |
| `docker compose config --quiet` | Passed |
| Final backend Compose rebuild and readiness | Passed: backend rebuilt with the final Phase 1.3 code and readiness returned HTTP 200 |
| Historical read APIs | Passed: sessions, results, laps, and positions endpoints each returned HTTP 200 for persisted fixture IDs |
| `docker compose up -d --build` | Passed: all 5 containers started successfully |
| Backend rebuild after Phase 1.2 changes | Passed: image built and backend container healthy |
| Java health/readiness | Passed: HTTP 200 |
| Python health | Passed: HTTP 200 |
| Frontend and frontend-to-Java proxy | Passed: HTTP 200 |
| Authenticated Java-to-Python health integration | Passed: HTTP 200 |
| Runtime reference-ingestion database verification | Passed: Flyway V2, 1 season, 1 manufacturer, 1 team, 2 riders, 1 circuit, 1 event, 7 source records, 7 identity mappings, 2 successful attempts |
| Flyway against pgvector PostgreSQL | Passed: V1 foundation migration successful |
| PostgreSQL foundation tables | Passed: 16 public tables including Flyway metadata |
| pgvector extension | Passed: version 0.8.7 |
| Redis | Passed: PONG |
| Fixture provider registration | Passed: `fixture-historical` present in PostgreSQL |
| Temporary-port Java/Python HTTP integration | Passed: Java status, authenticated Java-to-Python health, and Python health responses verified |
| Phase 1.4 backend Docker rebuild and runtime checks | Passed: backend image rebuilt, readiness returned HTTP 200, Compose configuration validated, and `/api/v1/seasons` and `/api/v1/events` returned HTTP 200 with persisted fixture data |
| Phase 1.4 pagination metadata tests | Passed: 14 controller tests including full-page, partial-final-page, and empty-page cases for laps and positions; `hasNext` uses the extra-row boundary and returned items remain limited to the requested page size |
| Phase 1.5 frontend TypeScript check | Passed: `npm test` |
| Phase 1.5 frontend production build | Passed: `npm run build` with Vite 6.4.6 |
| Phase 1.5 frontend Docker/runtime verification | Passed: frontend image built with the existing Dockerfile, container returned HTTP 200, and `/api/v1/seasons?limit=100&offset=0` proxied through Nginx with HTTP 200 and persisted data |
| Phase 1.6 synthetic fixture shape tests | Passed: provider tests verify 2 seasons, 2 manufacturers, 4 teams, 10 riders, 2 circuits, 4 events, 12 sessions, 28 relationships, 120 classifications, 400 laps, and 440 positions |
| Phase 1.6 Java verification | Passed: `mvn -B clean verify`, 47 tests, 0 failures/errors; 8 Compose-gated tests skipped by default |
| Phase 1.6 PostgreSQL ingestion integration | Passed explicitly against PostgreSQL 16.16: 7 tests, 0 failures/errors; exact entity counts, repeated-ingestion idempotency, conflict handling, and foreign-key relationship checks passed |
| Phase 1.6 PostgreSQL historical read integration | Passed explicitly against a fresh PostgreSQL 16.16 database: 1 test, 0 failures/errors; expanded season/event/session hierarchy, classification, lap/position ordering, pagination boundaries, and comparison retrieval passed |
| Phase 1.6 fixture refresh unit/integration verification | Passed: focused unit suite 14 tests, 0 failures/errors; isolated PostgreSQL refresh integration 1 test, 0 failures/errors; repaired changed classification/missing and corrupt lap, preserved unrelated-provider provenance, verified repeat refresh, rollback, and FAILED historical attempt persistence |
| Phase 1.6 Compose fixture refresh | Passed against the existing Compose database without volume reset: one-shot runner completed successfully; second run completed successfully; final counts are 2 seasons, 2 manufacturers, 4 teams, 10 riders, 2 circuits, 4 events, 12 sessions, 120 classifications, 400 laps, and 440 race positions |
| Phase 2.1 news unit verification | Passed: 5 tests covering RSS/Atom normalization, timestamps, image metadata, fallback fingerprints, malformed XML, duplicate collapse, and source failure handling |
| Phase 2.1 PostgreSQL news integration | Passed explicitly against PostgreSQL 16.16: 1 test; Flyway V4 applied, two fixture articles persisted, duplicate ingestion wrote zero new articles, source health and PARTIAL attempts recorded |

The frontend dependency installation reported four npm audit findings. They are not yet addressed because dependency remediation is outside the Phase 0 architecture scope and must be reviewed before changing versions.

## Commands executed

```text
cd backend/core && mvn -B clean verify
cd ai && python3 -m venv .venv && .venv/bin/python -m pip install -e '.[test]' && .venv/bin/python -m pytest
cd frontend/web && npm ci && npm run build && npm test
docker compose config --quiet
docker compose up -d --build
docker compose build backend
docker compose up -d backend
curl --fail http://127.0.0.1:8080/actuator/health/readiness
cd ai && .venv/bin/python -m pytest
cd frontend/web && npm run build && npm test
POSTGRES_PORT=55432 docker compose up -d postgres
mvn -B -Dtest=ReferenceDataComposeIntegrationTest -DrunComposeIntegrationTests=true -Dtest.db.url=jdbc:postgresql://127.0.0.1:55432/motogp_phase14c test
mvn -B -Dtest=HistoricalRaceReadStoreComposeIntegrationTest -DrunComposeIntegrationTests=true -Dtest.db.url=jdbc:postgresql://127.0.0.1:55432/motogp_phase14e test
POSTGRES_PORT=5432 docker compose up -d postgres
docker build --file frontend/web/Dockerfile --tag motogp-intelligence-web:phase0-fix .
docker compose exec -T redis redis-cli ping
docker compose exec -T postgres psql -U motogp -d motogp -Atc "select version || ':' || success from flyway_schema_history order by installed_rank; select code from providers where code='fixture-historical'; select count(*) from information_schema.tables where table_schema='public';"
git diff --check
```

## Outstanding issues

- Phase 1.2 has no authorized external provider integration; the fixture provider is intentionally deterministic and test-only.
- Sectors, tyres, weather, standings, raw payload capture, and public ingestion APIs remain future work; sessions/results/laps/positions are implemented only for the deterministic fixture provider.
- Review the four npm audit findings before production use.
- Replace the development fallback internal token in shared environments.
- Add full authentication, domain APIs, and persistence entities in later phases.

## Next action

Review the Phase 1.4 API implementation before beginning the next historical-data milestone. Do not add sectors, tyres, weather, standings, telemetry, or external providers without a separate approved scope.
