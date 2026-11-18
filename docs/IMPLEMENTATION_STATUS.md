# Implementation Status

Last reviewed: 2026-11-18

## Current state

Status: Phase 0, Phase 1.1, and Phase 1.2 historical reference-data ingestion milestones implemented; race-data ingestion has not started.

The approved Java-first modular monolith, Python AI service, React frontend shell, foundational Flyway migrations, Docker Compose configuration, and CI workflow are present. Phase 1.2 adds only deterministic fixture-based reference-data ingestion for seasons, manufacturers, teams, riders, circuits, and events. No race-result or lap-data ingestion, news ingestion, ML models, RAG, agents, simulation, or live timing has been implemented.

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
- Minimal dark application shell.
- Loading, unavailable, and error states.
- No mock MotoGP statistics.

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
| `mvn -B clean verify` | Passed: 16 tests, 0 failures, 0 errors, 1 explicitly gated Compose integration test skipped |
| Fixture ingestion unit tests | Passed: 5 tests covering ordering, idempotency, duplicate IDs, missing dependencies, and conflicts |
| Compose PostgreSQL ingestion integration test | Passed: Flyway V2, all 7 fixture records, valid event relationships, idempotent rerun, provenance, and 2 successful attempts |
| `python -m pytest` in isolated `.venv` | Passed: 3 tests, 0 failures |
| `npm run build` | Passed: TypeScript compilation and Vite production build |
| `npm test` | Passed: TypeScript check |
| Clean frontend `npm ci && npm run build && npm test` | Passed with locked versions: React Query 5.108.0, Vite 6.4.6, Rollup 4.68.6 |
| Frontend Docker image build | Passed: `motogp-intelligence-web:phase0-fix` |
| `docker compose config --quiet` | Passed |
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
cd ai && .venv/bin/python -m pytest
cd frontend/web && npm run build && npm test
POSTGRES_PORT=55432 docker compose up -d postgres
mvn -B -Dtest=ReferenceDataComposeIntegrationTest -DrunComposeIntegrationTests=true -Dtest.db.url=jdbc:postgresql://127.0.0.1:55432/motogp test
POSTGRES_PORT=5432 docker compose up -d postgres
docker build --file frontend/web/Dockerfile --tag motogp-intelligence-web:phase0-fix .
docker compose exec -T redis redis-cli ping
docker compose exec -T postgres psql -U motogp -d motogp -Atc "select version || ':' || success from flyway_schema_history order by installed_rank; select code from providers where code='fixture-historical'; select count(*) from information_schema.tables where table_schema='public';"
```

## Outstanding issues

- Phase 1.2 has no authorized external provider integration; the fixture provider is intentionally deterministic and test-only.
- Historical race results, sessions, laps, sectors, tyres, weather, standings, raw payload capture, and public ingestion APIs remain future work.
- Review the four npm audit findings before production use.
- Replace the development fallback internal token in shared environments.
- Add full authentication, domain APIs, and persistence entities in later phases.

## Next action

Stop at Phase 1.2. Do not begin race-result or lap-data ingestion until this reference-data milestone is reviewed.
