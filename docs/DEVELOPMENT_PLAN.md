# Phase 0 Execution Plan

Phase 0 foundation implementation is complete. The remaining phases retain their documented scope and have not been started.

## Sequence

1. Confirm repository ownership, Java/Python/Node versions, Docker availability, and provider decisions.
2. Approve architecture, module boundaries, ER diagram, and Java/Python contracts.
3. Create the monorepo skeleton and root development documentation.
4. Create the Java 21 Spring Boot modular-monolith skeleton with health, validation, error handling, and request correlation.
5. Create the Python FastAPI skeleton with `/health`, internal authentication, versioned routes, and contract tests.
6. Create the React/Vite shell with routing, API-client configuration, and explicit unavailable-data states.
7. Add PostgreSQL/pgvector and Redis through Docker Compose, with health checks and environment configuration.
8. Add only the foundational Flyway migration after the schema is approved.
9. Add CI for Java, Python, frontend, documentation checks, and service health smoke tests.
10. Validate local startup and authenticated Java-to-Python communication.
11. Update implementation status with files, tests, results, and remaining issues.

## Phase 0 deliverables

- Approved architecture and ADRs.
- Repository skeleton.
- Java project skeleton.
- Python project skeleton.
- React project skeleton.
- Docker Compose and environment template.
- Initial database migration.
- Health endpoints.
- Versioned Java/Python contract.
- CI pipeline.
- Developer setup and validation documentation.

## Acceptance criteria

- Services start locally with documented commands.
- Java, Python, PostgreSQL, Redis, and frontend health checks are observable.
- Flyway executes successfully against PostgreSQL with pgvector enabled.
- Java can call Python through authenticated `/internal/v1` contracts.
- No endpoint presents invented MotoGP data.
- Historical, recorded, and live modes are distinguishable in the design.
- CI runs relevant tests and fails on contract or build errors.
- Secrets are environment-provided and absent from Git.
- Documentation states every unsupported provider and feature.

## Out of scope for Phase 0

- Historical ingestion implementation.
- News scraping or publisher integration.
- Live timing integration.
- Production ML models.
- RAG corpus ingestion.
- Multi-agent execution.
- Race simulation implementation.
- Kubernetes deployment.
