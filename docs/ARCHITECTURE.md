# MotoGP Intelligence Architecture

Status: Approved baseline; Phase 0 foundation implemented.

## Repository baseline

The repository contains the initial README, `.gitignore`, and one Git initialization commit. There are currently no Java, Python, React, database, Docker, or deployment artifacts. The architecture below is therefore a target design, not a description of deployed components.

## Decisions

1. Use a Java 21 / Spring Boot modular monolith as the system of record and public API.
2. Use one separately deployable Python 3.11+ FastAPI service for ML, embeddings, RAG, agents, and AI-specific simulation.
3. Keep ingestion orchestration, scheduling, normalization, persistence, news storage, security, and business rules in Java.
4. Use PostgreSQL as the authoritative structured store, pgvector for document embeddings, and Redis only for transient live state and cacheable results.
5. Keep provider integrations behind Java adapters with provenance, retries, rate limits, timeouts, and fixtures.
6. Use recorded sessions and licensed/permitted historical data as the demonstrable fallback; live timing remains optional.
7. Do not add Kafka/Redpanda, Kubernetes, or additional deployable services before an evidenced need.

## Runtime boundaries

```text
External providers -> Java adapters -> PostgreSQL / Redis
                                  |
React + TypeScript <- Java public API -> Python internal API
                                      |
                              ML / RAG / agents
```

The frontend calls Java only. Java validates requests, applies domain rules, queries structured analytics, and calls Python through authenticated versioned contracts. Python never receives unrestricted database credentials and does not own application business logic.

## Java modules

| Module | Owns |
|---|---|
| common | Errors, envelopes, pagination, clocks, correlation IDs, observability |
| identity | Users, roles, JWT, refresh tokens, preferences |
| race | Seasons, events, sessions, results, replay |
| rider | Rider profiles and statistics |
| team | Teams and manufacturers |
| circuit | Circuit metadata |
| analytics | Read-only allowlisted historical analytics |
| ingestion | Provider ports, scheduling, normalization, provenance |
| news | Sources, articles, stories, ranking, enrichment orchestration |
| prediction | Prediction requests, model metadata, result persistence |
| ai-gateway | Python calls, chat sessions, citations, traces |
| simulation | Simulation orchestration and persistence |
| live | Authorized live adapter, Redis state, SSE |

Modules communicate through application services and ports. Controllers must not access repositories directly.

## Python modules

`api`, `ml`, `news`, `rag`, `agents`, `simulation`, and `common` are internal packages. ML training/evaluation artifacts remain separate from inference code. Agent tools expose typed, allowlisted operations only. Every result carries model/contract version and evidence or limitations.

## Availability modes

The product must support three explicit modes:

- historical: structured database and analytics available;
- recorded: replay data used when live access is unavailable;
- live: current data shown only when freshness and provider authorization are valid.

The UI must not label recorded or stale data as live.

## Main risks

- The repository has no existing MotoGP components to reuse.
- Live timing and detailed telemetry require authorization or licensing.
- Historical lap, sector, tyre, and weather coverage will be uneven.
- News ingestion must respect publisher terms and preserve attribution.
- ML validation must be chronological to prevent leakage.
- Agent-generated numbers must come from structured queries or model outputs.
