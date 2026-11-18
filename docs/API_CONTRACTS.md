# API Contracts

Status: Approved baseline; Phase 0 system and internal health contracts implemented.

## Public Java API

Base path: `/api/v1`.

Public APIs include seasons, events, sessions, riders, teams, circuits, news, predictions, AI sessions, simulations, and live session state. The full endpoint inventory is defined by the master specification.

Every response uses a consistent envelope:

```json
{
  "requestId": "uuid",
  "data": {},
  "page": null,
  "errors": []
}
```

Errors contain a stable code, human-readable message, field errors where applicable, and the request ID. Pagination must be consistent across list endpoints.

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
