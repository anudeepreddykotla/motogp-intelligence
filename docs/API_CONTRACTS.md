# API Contracts

Status: Approved baseline; Phase 2.1 news ingestion foundation implemented without a public news ingestion endpoint.

## Public Java API

Base path: `/api/v1`.

Public APIs include seasons, events, sessions, riders, teams, circuits, news, predictions, AI sessions, simulations, and live session state. The full endpoint inventory is defined by the master specification.

Every response uses a consistent envelope:

```json
{
  "requestId": "uuid",
  "data": {},
  "errors": []
}
```

Errors contain a stable code, human-readable message, field errors where applicable, and the request ID. Pagination must be consistent across list endpoints.

## Historical race read APIs

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/seasons?limit=100&offset=0` | Available seasons, newest first |
| GET | `/api/v1/seasons/{seasonId}` | Season details |
| GET | `/api/v1/events?seasonId={seasonId}&limit=100&offset=0` | Events, optionally filtered by season |
| GET | `/api/v1/events/{eventId}` | Event details, including circuit and season |
| GET | `/api/v1/events/{eventId}/sessions?limit=100&offset=0` | Sessions belonging to an event |
| GET | `/api/v1/sessions/{sessionId}` | Session details |
| GET | `/api/v1/sessions/{sessionId}/results?classificationStatus=CLASSIFIED&limit=500&offset=0` | Rider classifications for a session |
| GET | `/api/v1/riders/compare?sessionId={sessionId}&riderIds={riderId}&riderIds={riderId}` | Session-scoped comparison of 2–4 persisted rider classifications |
| GET | `/api/v1/sessions/{sessionId}/laps?riderId={riderId}&limit=1000&offset=0` | Rider lap records |
| GET | `/api/v1/sessions/{sessionId}/positions?riderId={riderId}&limit=1000&offset=0` | Rider race-position snapshots |

These endpoints return persisted records only and use the existing `{requestId,data,errors}` response envelope. The paginated season, event, event-session, session-result, lap, and position responses use this `data` shape:

```json
{
  "items": [],
  "limit": 100,
  "offset": 0,
  "hasNext": false
}
```

`hasNext` is determined by reading at most one record beyond the requested limit; the extra record is never returned. Ordering remains stable, and no total-count query is performed. Invalid pagination, an invalid comparison cardinality, or an unsupported status value returns `400`; missing season, event, session, or rider resources return `404`. They are temporarily public in the local Phase 1 environment because user authentication is not implemented. Sectors, tyres, weather, standings, telemetry, and replay are deliberately absent.

The comparison endpoint is intentionally session-scoped in this phase. It returns only stored classification fields and does not invent lap, sector, or statistical values. A rider with no classification in the selected session is represented by an absent comparison row; a nonexistent rider is a `404`.

Lap `observedAt` may be `null` when a provider supplies a lap time without an observation timestamp. The lap endpoint still returns HTTP 200 and JSON `null`; the service does not infer a timestamp from session start, lap number, or any other field.

## News ingestion operations

Phase 2.1 does not expose a public ingestion API. News ingestion is an operator-only Java operation using the `news-ingestion` profile and `motogp.news.run=true`. Configured sources are fetched through Java RSS/Atom adapters; only normalized metadata and permitted excerpts are stored. News listing and enrichment APIs remain future work.

## Internal Python API

Base path: `/internal/v1`.

| Group | Endpoints |
|---|---|
| ML | `/ml/lap-time/predict`, `/ml/pace/analyze`, `/ml/race/predict`, `/ml/rider/similarity` |
| News | `/news/classify`, `/news/importance`, `/news/summarize`, `/news/entities` |
| RAG | `/rag/ingest`, `/rag/search`, `/rag/query` |
| Agents | `/agents/invoke`, `/agents/executions/{id}` |
| Simulation | `/simulation/run` |

Python requests and responses use Pydantic schemas. Internal authentication, timeout, idempotency where needed, explicit model version, and contract version are mandatory.

## Boundary rules

- Java owns public validation, authorization, persistence, and domain decisions.
- Python owns model inference, embeddings, retrieval, agent orchestration, and AI-specific simulation.
- Java passes selected structured facts to Python; Python must not query arbitrary application tables.
- Java validates Python output before persistence or presentation.
- Numerical claims must include provenance such as source record IDs, prediction run IDs, or document citations.
- Missing features must be represented explicitly; missing telemetry must never be fabricated.

## Example prediction contract

Request:

```json
{
  "contractVersion": "1.0",
  "modelVersion": "lap-time-xgb-2026-01",
  "riderId": "uuid",
  "sessionId": "uuid",
  "currentLap": 12,
  "recentLapTimesMs": [92410, 92380, 92520],
  "features": {"weather": null, "tyre": "SOFT", "sectorTimesAvailable": true}
}
```

Response:

```json
{
  "contractVersion": "1.0",
  "predictedLapTimeMs": 92395,
  "intervalMs": {"lower": 92120, "upper": 92780},
  "confidence": 0.71,
  "modelVersion": "lap-time-xgb-2026-01",
  "featureAvailability": {"weather": false, "tyre": true, "sectorTimes": true},
  "limitations": ["weather unavailable"]
}
```
