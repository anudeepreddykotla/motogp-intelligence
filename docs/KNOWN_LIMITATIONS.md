# Known Limitations

- No existing MotoGP Java, Go, frontend, ingestion, database, or deployment implementation was found in this repository.
- The initial repository has no confirmed historical dataset.
- Live timing access is not established and must not be assumed.
- Detailed telemetry such as throttle, braking, lean angle, suspension, and GPS is not guaranteed to be public or licensed.
- Historical lap, sector, tyre, and weather fields may be incomplete by season and provider.
- News ingestion, article summaries, and document embeddings depend on publisher permissions and source terms.
- ML models cannot be considered valid until chronological evaluation, baselines, calibration, and versioned artifacts exist.
- AI responses cannot be considered grounded until structured tools, retrieval, citations, and failure handling are tested.
- License terms for datasets, third-party APIs, and the project itself remain unresolved.
- Deployment target, secret management, and operational cost limits remain unresolved.
- User authentication, JWT issuance, refresh-token rotation, and role management are not implemented in Phase 0.
- The local Compose fallback token is intentionally development-only and must be replaced through environment configuration before shared or deployed use.
- CI validates individual Java, Python, frontend, and frontend-container builds; full Compose integration remains an environment-level verification step.
- Phase 1.1 includes only the deterministic `fixture-historical` provider; no external API, web-scraping adapter, or licensed historical dataset is integrated.
- Provider identity mapping is an internal Java service boundary and is not yet exposed as a public or frontend API.
- Identity mappings validate entity existence against the approved allowlisted domain tables; the generic `internal_id` design intentionally does not use polymorphic foreign keys.
- Historical race results, sessions, laps, sectors, tyres, weather, standings, raw payload capture, and public ingestion APIs are not implemented yet.
- Phase 1.2 ingests only the fixture provider’s seven normalized reference records; it does not yet ingest team/manufacturer affiliations or rider season entries.
