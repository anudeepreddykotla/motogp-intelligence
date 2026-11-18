# Architecture Decisions

## ADR-001: Java modular monolith first

Status: Accepted

Decision: The main application will be one Java 21 / Spring Boot modular monolith.

Rationale: The specification makes Java responsible for public APIs, domain logic, persistence, ingestion orchestration, scheduling, security, and analytics. A modular monolith keeps those boundaries explicit without premature service-to-service operational complexity.

Consequences: Modules must have clear ownership and dependency rules. Extraction into services remains possible later, but is not a Phase 0 goal.

## ADR-002: Single Python AI service

Status: Accepted

Decision: ML, inference, embeddings, RAG, agents, and AI-specific simulation run in one separately deployable Python service.

Rationale: These responsibilities share model artifacts and Python libraries, while the specification explicitly separates AI concerns from the Java system.

Consequences: Java/Python contracts require versioning, authentication, timeouts, validation, and observability.

## ADR-003: Provider adapters and recorded fallback

Status: Accepted

Decision: External race and news sources are isolated behind adapters, with deterministic fixtures and recorded sessions as the fallback.

Rationale: Live and licensed data access is uncertain; core product value must remain available without an unofficial endpoint or LLM provider.

Consequences: Provenance, freshness, coverage, and source failure states are first-class data.

## ADR-004: PostgreSQL plus Redis, no event broker initially

Status: Accepted

Decision: PostgreSQL is authoritative and Redis serves transient live state/cache. Kafka/Redpanda is deferred.

Rationale: The initial workloads are scheduled ingestion, historical queries, news enrichment, and optional live updates. An event broker is not justified before measured throughput or replay requirements demand it.

Consequences: Background jobs should be idempotent and observable; an event-driven migration may be documented later if needed.

## ADR-005: No unauthorized live-data dependency

Status: Accepted

Decision: Unofficial MotoGP endpoints and scraping cannot be the production dependency for live intelligence.

Rationale: Availability, terms, redistribution rights, and schema stability are not assured.

Consequences: Live mode may remain disabled until an authorized provider is available; recorded mode must be demonstrable.
