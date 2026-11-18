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
