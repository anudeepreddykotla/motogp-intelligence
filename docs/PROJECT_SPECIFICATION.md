# MotoGP Intelligence Platform
## Master Technical Specification and Implementation Contract

**Project:** MotoGP Intelligence
**Type:** Full-stack AI-powered motorsport intelligence platform
**Architecture:** Java-first backend with Python AI/ML services
**Primary language:** Java 21
**AI language:** Python 3.11+
**Frontend:** React + TypeScript
**Database:** PostgreSQL + pgvector
**Cache:** Redis
**Deployment:** Docker Compose initially, Kubernetes-ready architecture

---

# 1. PROJECT OBJECTIVE

Build a production-oriented MotoGP intelligence platform that combines historical race analytics, live race monitoring, MotoGP news aggregation, predictive machine learning, retrieval-augmented generation, multi-agent AI and race simulation.

The platform should function as an intelligent MotoGP analytics and decision-support system.

It should allow users to:

1. Monitor ongoing MotoGP sessions when an authorized live data source is available.
2. Explore historical races, riders, teams, manufacturers and circuits.
3. Compare rider performance using statistical analytics.
4. Predict lap times, rider performance trends and race outcomes.
5. Read the five or ten most important MotoGP news updates.
6. Ask natural-language questions about historical and live race information.
7. Obtain evidence-backed answers using SQL analytics, ML predictions and RAG.
8. Simulate alternative race scenarios and championship outcomes.

The application must remain useful when live feeds or external AI providers are unavailable.

## 1.1 Core principles

- Java owns the main application and system implementation.
- Python owns AI, ML, embeddings, RAG and agent orchestration.
- The frontend communicates with the Java backend.
- Java communicates with Python through internal HTTP APIs.
- PostgreSQL is the authoritative store for structured historical data.
- Redis stores transient live state and frequently accessed results.
- All external data sources must be isolated behind adapters.
- ML predictions must be measurable and reproducible.
- AI-generated answers must distinguish facts from predictions and interpretations.
- News must have source attribution and original article links.
- Every major service must have clear interfaces and automated tests.
- No mandatory dependency on unofficial or unauthorized MotoGP endpoints.

# 2. SYSTEM ARCHITECTURE

## 2.1 High-level architecture

The application consists of the following logical components.

### Frontend
React + TypeScript.

Responsibilities:
- Dashboard.
- Live race monitoring.
- Historical race exploration.
- Rider analytics.
- News feed.
- AI Race Engineer interface.
- Predictions.
- Simulations.
- User preferences.

### Core backend
Java 21 + Spring Boot.

Responsibilities:
- REST API.
- Authentication and authorization.
- User management.
- Race, rider, team and circuit management.
- Data ingestion orchestration.
- News ingestion and persistence.
- Analytics queries.
- Cache management.
- Background scheduling.
- AI service integration.
- API validation and error handling.

### AI service
Python + FastAPI.

Responsibilities:
- Model training pipelines.
- ML inference.
- Feature engineering.
- Prediction evaluation.
- News classification and ranking assistance.
- Embeddings.
- RAG retrieval.
- Agent orchestration.
- LLM tool calling.
- Race simulation support where ML-related.

### Infrastructure

- PostgreSQL with pgvector.
- Redis.
- Docker Compose.
- Prometheus.
- Grafana.
- OpenTelemetry.
- Optional Redpanda/Kafka in later releases.

## 2.2 Target architecture

```text
                    EXTERNAL DATA
                           |
          +----------------+----------------+
          |                |                |
     Race Data         News Sources     Documents
          |                |                |
          +----------------+----------------+
                           |
                  JAVA INGESTION LAYER
                           |
              +------------+-------------+
              |                          |
       Historical Data              News Pipeline
              |                          |
              +------------+-------------+
                           |
                 POSTGRESQL + PGVECTOR
                           |
              +------------+-------------+
              |                          |
            REDIS                  DOCUMENT INDEX
              |                          |
              +------------+-------------+
                           |
                  JAVA SPRING BOOT
                           |
              +------------+-------------+
              |            |              |
          Analytics     News API       AI Gateway
              |            |              |
              +------------+--------------+
                           |
                    PYTHON FASTAPI
                           |
        +------------------+------------------+
        |                  |                  |
    ML Models          RAG Engine        Agent System
        |                  |                  |
        +------------------+------------------+
                           |
                   AI RESPONSE
                           |
                    REACT FRONTEND
```

## 2.3 Language boundaries

Java must implement:

- All public application APIs.
- Core domain entities and business rules.
- User authentication.
- Data ingestion scheduling.
- External provider adapters.
- News ingestion and storage.
- Data validation.
- Historical analytical queries.
- Redis integration.
- AI gateway.
- Model request orchestration.
- Background jobs.
- System monitoring and health checks.

Python must implement:

- Model training and evaluation.
- ML inference endpoints.
- Feature transformations specific to ML.
- Embedding generation.
- Vector retrieval.
- News classification.
- News importance scoring assistance.
- Agent orchestration using LangGraph.
- LLM integration.
- Simulation algorithms that are part of the AI subsystem.

Do not implement application business logic in Python simply because an AI operation is involved.

Do not implement ML model inference in Java.

# 3. TECHNOLOGY STACK

## 3.1 Java backend

- Java 21.
- Spring Boot 3.x.
- Spring Web.
- Spring Security.
- Spring Data JPA.
- Spring Data Redis.
- Spring Validation.
- Spring Actuator.
- Spring Scheduler.
- Spring WebFlux only where reactive streaming is genuinely useful.
- Flyway for database migrations.
- Maven.
- JUnit 5.
- Mockito.
- Testcontainers.

Use a modular monolith for the core Java application initially.

Organize modules by domain rather than creating an excessive number of microservices.

Suggested modules:

1. identity
2. race
3. rider
4. team
5. circuit
6. analytics
7. ingestion
8. news
9. prediction
10. ai-gateway
11. simulation
12. common

## 3.2 Python AI service

- Python 3.11 or later.
- FastAPI.
- Pydantic.
- pandas.
- NumPy.
- scikit-learn.
- XGBoost.
- LightGBM.
- PyTorch for optional deep learning.
- LangGraph.
- LangChain components where useful.
- sentence-transformers or an embedding API.
- PostgreSQL/pgvector integration.
- MLflow.
- pytest.

Avoid unnecessary frameworks and overlapping abstractions.

## 3.3 Frontend

- React.
- TypeScript.
- Vite.
- React Router.
- TanStack Query.
- Tailwind CSS.
- Recharts.
- A suitable component library.
- Server-Sent Events for live updates.

## 3.4 Infrastructure

- PostgreSQL.
- pgvector extension.
- Redis.
- Docker.
- Docker Compose.
- Prometheus.
- Grafana.
- OpenTelemetry.
- GitHub Actions.

Redpanda/Kafka is a later-stage enhancement, not an initial prerequisite.

# 4. FUNCTIONAL MODULES

## 4.1 Home dashboard

The dashboard must provide a consolidated view of MotoGP activity.

Components:

- Latest MotoGP news: top five stories.
- Current or upcoming Grand Prix.
- Latest race results.
- Championship standings.
- Featured rider comparisons.
- Recent predictions.
- Live session indicator.
- AI Race Engineer entry point.

If there is no ongoing race, display the latest completed session and upcoming event.

Never display stale live data as current.

## 4.2 Historical race explorer

Users must be able to explore:

- Seasons.
- Grand Prix events.
- Circuits.
- Sessions.
- Riders.
- Teams.
- Manufacturers.
- Session classifications.
- Lap times.
- Sector times where available.
- Tyre information where available.
- Top speeds where available.

Required functionality:

- Search and filtering.
- Rider comparisons.
- Lap-time charts.
- Sector comparisons.
- Position progression.
- Race replay.
- Session summaries.

Historical replay must reconstruct race state from timestamped events or lap-level snapshots.

## 4.3 Live race intelligence

Support live race information through an interchangeable provider adapter.

Requirements:

- Current session information.
- Rider positions.
- Lap numbers.
- Lap times.
- Gaps.
- Sector information where available.
- Session status.
- Timestamp of the latest update.

Use Redis for current live state.

Use Server-Sent Events for frontend updates.

The system must support recorded-session replay when live access is unavailable.

Never fabricate missing telemetry.

Do not assume throttle, braking, lean angle, suspension or GPS telemetry is publicly accessible.

## 4.4 MotoGP news and updates

This is a mandatory module.

### Sources

Implement a configurable source registry.

Initial source candidates:

1. Official MotoGP latest-news page.
2. Crash.net MotoGP coverage.
3. Other reputable motorsport publications.
4. Authorized RSS/Atom feeds.
5. Licensed news APIs, if available.

Use official or licensed feeds where possible.

Website-specific adapters must comply with publisher terms and access restrictions.

Do not depend on unofficial scraping as the only source.

### News functionality

The system must provide:

- Top five important updates on the dashboard.
- Top ten important updates on the news page.
- Chronological latest-news feed.
- Category filtering.
- Rider filtering.
- Team filtering.
- Manufacturer filtering.
- Source filtering.
- Article search.
- Original source links.
- AI-generated summaries.
- Publication timestamps.
- Importance scores.

### News categories

- RACE_RESULTS
- RIDER_NEWS
- INJURY_AND_RECOVERY
- TEAM_NEWS
- RIDER_TRANSFERS
- TECHNICAL
- CHAMPIONSHIP
- RACE_WEEKEND
- REGULATIONS
- BUSINESS
- INTERVIEWS
- GENERAL

### News ingestion workflow

1. Java scheduler triggers source polling.
2. Source adapters retrieve permitted article metadata.
3. Normalize title, URL, source, published date and available content.
4. Validate required fields.
5. Canonicalize article URLs.
6. Detect duplicate articles.
7. Persist normalized records.
8. Send new or changed articles to Python for classification.
9. Extract riders, teams, manufacturers and events.
10. Calculate importance score.
11. Generate a short summary from permitted retrieved content.
12. Store enrichment results.
13. Expose ranked news through Java APIs.

### News importance ranking

Create a score from 0 to 100.

Initial weighted scoring:

- Source credibility: 20%.
- Category importance: 25%.
- Championship/race impact: 25%.
- Recency: 20%.
- Independent corroboration: 10%.

The scoring model must be configurable.

Use recency decay rather than a simple publication-date sort.

For the initial implementation, use deterministic scoring with Python-assisted classification.

### Duplicate story handling

Two different articles may describe the same event.

Implement story grouping.

Each story can have multiple source articles.

Use:

- Canonical URLs.
- Title similarity.
- Entity overlap.
- Publication-time proximity.
- Semantic similarity as an optional enhancement.

Do not allow repeated coverage of one event to occupy the entire Top 10.

### News API

- GET /api/v1/news
- GET /api/v1/news/top
- GET /api/v1/news/{id}
- GET /api/v1/news/stories/{id}
- GET /api/v1/news/categories
- GET /api/v1/news/search

Supported parameters:

- limit
- page
- category
- riderId
- teamId
- source
- from
- to
- sort

The top endpoint must support a limit of 5 or 10.

Every article response must contain:

- id
- storyId
- title
- canonicalUrl
- sourceName
- publishedAt
- fetchedAt
- category
- importanceScore
- summary
- riderTags
- teamTags
- manufacturerTags
- verificationStatus

The frontend must visibly distinguish AI-generated summaries from publisher headlines.

## 4.5 Rider analytics

Implement:

- Career statistics.
- Season statistics.
- Race results.
- Qualifying results.
- Sprint performance.
- Average finishing position.
- Podium rate.
- Win rate.
- DNF rate.
- Lap-time consistency.
- Circuit-specific performance.
- Recent performance trends.

Comparison should allow at least two riders.

All calculations must be based on actual stored records.

## 4.6 Predictive analytics

Implement four initial models.

### Model 1: Lap-time prediction

Input:
- Rider.
- Circuit.
- Session.
- Current lap.
- Previous lap times.
- Rolling averages.
- Lap-time variance.
- Recent sector times where available.
- Position.
- Gap.
- Race phase.
- Weather where available.
- Tyre information where available.

Output:
- Predicted next-lap time.
- Prediction interval.
- Model version.
- Feature availability.
- Confidence/calibration information.

Primary algorithm: XGBoost or LightGBM.

Baselines:
- Previous lap time.
- Rolling average.
- Historical average for the same race phase.

Evaluate with MAE and RMSE.

### Model 2: Pace degradation detection

Identify abnormal changes in rider performance.

Techniques:
- Rolling statistical analysis.
- Isolation Forest.
- Change-point detection.
- Gradient boosting classification when labelled data supports it.

Output:
- NORMAL
- IMPROVING
- DETERIORATING
- ANOMALOUS

Include a reason code and supporting measurements.

Avoid claiming tyre degradation unless tyre data and a validated model support that conclusion.

### Model 3: Race outcome prediction

Predict:
- Finishing-position distribution.
- Top-three probability.
- Top-five probability.
- DNF probability where data supports it.
- Expected finishing position.

Use LightGBM/XGBoost with probability calibration.

Use historical data and strictly time-aware validation.

Avoid data leakage from post-race information.

Evaluate using:
- Log loss.
- Brier score.
- Calibration.
- Ranking quality.
- Top-three classification metrics.

### Model 4: Rider similarity

Use engineered rider performance features.

Algorithms:
- StandardScaler.
- PCA.
- K-Means or Gaussian Mixture Models.

Return similar riders and feature-level explanations.

Do not present cluster labels as objective racing truths.

## 4.7 AI Race Engineer

Implement a multi-agent system using LangGraph.

Agents:

1. Race Director.
2. Performance Analyst.
3. Prediction Analyst.
4. Research Analyst.
5. Strategy Analyst.

### Race Director

Responsibilities:
- Receive user questions.
- Identify entities and analytical intent.
- Select required agents.
- Manage shared state.
- Enforce execution limits.
- Combine results.
- Produce the final structured response.

### Performance Analyst

Tools:
- Historical SQL analytics.
- Rider comparison.
- Lap-time analysis.
- Sector analysis.
- Position progression.

### Prediction Analyst

Tools:
- Lap-time prediction.
- Pace degradation.
- Race outcome prediction.
- Prediction uncertainty.

### Research Analyst

Tools:
- RAG retrieval.
- Circuit knowledge.
- Historical reports.
- Rider and team information.
- Technical documents.
- Relevant news articles.

### Strategy Analyst

Responsibilities:
- Combine results.
- Compare scenarios.
- Evaluate trade-offs.
- Identify risks.
- Generate evidence-backed interpretations.

### Agent execution requirements

- Use typed shared state.
- Use structured tool inputs and outputs.
- Run independent analyses in parallel when possible.
- Enforce per-agent timeouts.
- Limit graph iterations.
- Handle failed tools gracefully.
- Preserve conversation context.
- Store execution traces.
- Prevent arbitrary SQL execution.
- Do not allow the LLM to invent numerical results.

The agent must cite the underlying race records, prediction results or documents it uses.

### Example supported questions

- Compare two riders at a particular circuit.
- Explain a rider's pace deterioration.
- Predict the next lap time.
- Explain championship implications.
- Summarize important updates about a rider.
- Compare qualifying and race performance.
- Analyze a historical race.
- Investigate why a rider lost positions.
- Simulate alternative finishing positions.

# 5. RAG SYSTEM

Use PostgreSQL with pgvector.

### Knowledge sources

- Public race reports.
- Published session reports.
- Circuit information.
- Rider profiles.
- Team information.
- Technical regulations.
- Historical race summaries.
- News articles and permitted extracts.

### Ingestion

1. Fetch or upload documents.
2. Extract text.
3. Preserve document metadata.
4. Split into semantic chunks.
5. Generate embeddings.
6. Store vectors and metadata.
7. Maintain document versions.

### Retrieval

Use:
- Metadata filtering.
- Semantic vector retrieval.
- Full-text search.
- Hybrid retrieval.
- Reranking where justified.

Metadata must include:

- documentId
- source
- documentType
- season
- eventId
- sessionId
- riderId
- teamId
- publishedAt
- pageNumber
- chunkIndex

### Grounding

Every RAG answer must identify its supporting sources.

If retrieval returns insufficient evidence, the agent must state that the information is unavailable.

Do not use RAG for numerical lap-time calculations. Use structured SQL analytics for those.

# 6. RACE SIMULATION

Implement a Monte Carlo-based simulator.

Inputs:
- Starting grid.
- Rider performance distributions.
- Predicted race pace.
- DNF probability where validated.
- Circuit characteristics.
- Race distance.
- Scenario parameters.

Outputs:
- Finishing-position distributions.
- Podium probabilities.
- Championship points distributions.
- Scenario comparisons.

Support historical backtesting.

The simulator must use the actual points system applicable to the selected season.

Do not claim that hypothetical simulation outputs are guaranteed outcomes.

Reinforcement learning is explicitly outside the initial scope.

# 7. DATA MODEL

Use PostgreSQL as the primary relational database.

Core entities:

### Reference data

- seasons
- events
- circuits
- riders
- teams
- manufacturers
- sessions

### Race data

- session_results
- rider_laps
- rider_sectors
- race_positions
- tyre_stints
- weather_observations
- championship_standings

### News data

- news_sources
- news_articles
- news_stories
- news_entities
- news_enrichments

### AI and ML

- model_registry
- prediction_runs
- predictions
- ml_evaluation_runs
- documents
- document_chunks
- agent_sessions
- agent_execution_traces

### Identity

- users
- roles
- refresh_tokens
- user_preferences

Important design requirements:

- Use UUID identifiers where appropriate.
- Use UTC timestamps.
- Store race-local timezone information when relevant.
- Preserve original source identifiers.
- Add unique constraints for external records.
- Index frequently queried foreign keys.
- Add composite indexes for session/rider/lap queries.
- Use Flyway migrations.
- Avoid storing derived values as authoritative source data.
- Keep raw source records separate from normalized records.

Create an ER diagram before implementing the database.

# 8. JAVA API SPECIFICATION

Base path: /api/v1

### Seasons and events

- GET /seasons
- GET /seasons/{id}
- GET /events
- GET /events/{id}
- GET /events/{id}/sessions

### Riders

- GET /riders
- GET /riders/{id}
- GET /riders/{id}/statistics
- GET /riders/{id}/analytics
- GET /riders/compare

### Teams and circuits

- GET /teams
- GET /teams/{id}
- GET /circuits
- GET /circuits/{id}

### Sessions

- GET /sessions/{id}
- GET /sessions/{id}/results
- GET /sessions/{id}/laps
- GET /sessions/{id}/positions
- GET /sessions/{id}/replay

### News

- GET /news
- GET /news/top
- GET /news/{id}
- GET /news/stories/{id}
- GET /news/categories
- GET /news/search

### Predictions

- POST /predictions/lap-time
- POST /predictions/pace-analysis
- POST /predictions/race-outcome
- GET /predictions/{id}
- GET /predictions/models

### AI

- POST /ai/chat
- GET /ai/sessions
- GET /ai/sessions/{id}
- GET /ai/sessions/{id}/traces

### Simulation

- POST /simulations
- GET /simulations/{id}
- GET /simulations/{id}/results

### Live data

- GET /live/sessions/{id}
- GET /live/sessions/{id}/stream

All APIs must use consistent response envelopes, validation errors and pagination.

# 9. PYTHON API CONTRACT

Internal base path: /internal/v1

### ML

- POST /ml/lap-time/predict
- POST /ml/pace/analyze
- POST /ml/race/predict
- POST /ml/rider/similarity

### News intelligence

- POST /news/classify
- POST /news/importance
- POST /news/summarize
- POST /news/entities

### RAG

- POST /rag/ingest
- POST /rag/search
- POST /rag/query

### Agents

- POST /agents/invoke
- GET /agents/executions/{id}

### Simulation

- POST /simulation/run

Use Pydantic request and response schemas.

All endpoints must support explicit version identifiers where model or contract compatibility matters.

Internal service authentication is mandatory.

# 10. FRONTEND SPECIFICATION

Build the following pages.

1. Home Dashboard.
2. Live Race.
3. Historical Races.
4. Race Details.
5. Rider Profiles.
6. Rider Comparison.
7. Team and Manufacturer Profiles.
8. News Feed.
9. News Article Details.
10. Predictions.
11. AI Race Engineer.
12. Race Simulator.
13. User Settings.

### Home Dashboard

Must include:
- Top five news updates.
- Upcoming Grand Prix.
- Latest results.
- Championship standings.
- Recent prediction highlights.
- Live session status.

### News page

Must include:
- Top ten important stories.
- Latest chronological feed.
- Category filters.
- Rider and team filters.
- Search.
- Original article links.
- Publication times.
- Importance indicators.

### AI interface

Must include:
- Chat history.
- Conversation context.
- Streaming responses if supported.
- Analytical result cards.
- Charts.
- Citations.
- Prediction confidence information.
- Agent execution details in an expandable technical view.

### Design direction

Use a professional motorsport analytics aesthetic.

- Dark-first interface.
- Clear information hierarchy.
- Compact data tables.
- Responsive charts.
- Consistent colors for riders and teams.
- Clear status indicators.
- Minimal unnecessary animations.
- Accessible contrast.
- Responsive desktop and mobile layouts.

Do not sacrifice readability for visual styling.

# 11. DATA SOURCES AND PROVIDER ADAPTERS

Create interfaces for external data providers.

### Race data

Potential sources:

- Public historical MotoGP datasets.
- Published timing documents.
- MotoGP official results and timing services where permitted.
- Licensed providers such as Sportradar.
- User-provided CSV files for fallback ingestion.

### News

Potential sources:

- Official MotoGP news.
- Crash.net.
- Other reputable motorsport publications.
- RSS/Atom.
- Licensed news APIs.

### Source adapter rules

Each adapter must:
- Have a configurable enable/disable flag.
- Implement connection timeouts.
- Implement retry policies.
- Respect rate limits.
- Normalize provider-specific records.
- Record ingestion failures.
- Preserve provenance.
- Support test fixtures.

Do not hard-code provider-specific fields into core domain entities.

Do not make the application depend on one unofficial endpoint.

For unavailable providers, provide sample datasets and deterministic replay.

# 12. SECURITY AND RELIABILITY

Implement:

- Spring Security.
- JWT access tokens.
- Refresh token rotation.
- Role-based access.
- Password hashing.
- Request validation.
- API rate limiting.
- Internal service authentication.
- Secrets through environment configuration.
- SQL injection protection.
- Safe LLM tool execution.
- Document and prompt injection defenses.
- CORS configuration.
- Audit logging for sensitive operations.

Agent tools must have restricted capabilities.

The AI must never receive unrestricted database credentials.

All database access must use read-only, allowlisted analytics operations.

Add timeouts and circuit breakers for external APIs and AI providers.

Core historical analytics must continue to work when the LLM provider is unavailable.

# 13. TESTING STRATEGY

## Java

- Unit tests.
- Repository integration tests.
- Controller tests.
- Service integration tests.
- Testcontainers for PostgreSQL and Redis.
- Authentication tests.
- Provider adapter tests.
- API contract tests.

## Python

- Unit tests for features.
- Model inference tests.
- Model evaluation tests.
- RAG retrieval tests.
- Agent routing tests.
- Tool validation tests.
- API integration tests.
- Failure and timeout tests.

## Frontend

- Component tests.
- API integration tests.
- User workflow tests.
- Error-state tests.

## End-to-end

Test these complete scenarios:

1. Import a historical race.
2. Open its results.
3. Compare two riders.
4. Generate a lap-time prediction.
5. Ask the AI agent to explain the prediction.
6. Retrieve a cited race report.
7. Open the news feed.
8. Run a race simulation.

# 14. OBSERVABILITY

Use Prometheus, Grafana and OpenTelemetry.

Track:

### Application metrics
- API request count.
- Request latency.
- HTTP error rates.
- Database connection pool usage.
- Cache hit rate.
- Ingestion success/failure.

### AI metrics
- Model inference latency.
- Prediction request count.
- LLM token consumption.
- Agent execution time.
- Tool failure rate.
- RAG retrieval latency.
- Embedding generation failures.

### News metrics
- Articles fetched.
- Articles successfully processed.
- Duplicate rate.
- Source freshness.
- Classification failures.
- Summary generation failures.

### ML metrics
- Prediction MAE.
- Prediction RMSE.
- Brier score.
- Model version usage.
- Prediction drift indicators.

Never log passwords, access tokens or sensitive provider credentials.

# 15. PROJECT STRUCTURE

Use a monorepo.

```text
motogp-intelligence/
|
├── frontend/
│   └── web/
|
├── backend/
│   └── core/
│       ├── src/main/java/
│       ├── src/main/resources/
│       └── src/test/
|
├── ai/
│   ├── app/
│   │   ├── api/
│   │   ├── agents/
│   │   ├── rag/
│   │   ├── news/
│   │   ├── ml/
│   │   ├── simulation/
│   │   └── common/
│   |
│   ├── training/
│   ├── evaluation/
│   ├── tests/
│   └── requirements/
|
├── data-ingestion/
│   ├── historical/
│   ├── live/
│   ├── news/
│   └── documents/
|
├── datasets/
│   ├── raw/
│   ├── processed/
│   └── fixtures/
|
├── infrastructure/
│   ├── docker/
│   ├── monitoring/
│   ├── postgres/
│   └── kubernetes/
|
├── docs/
│   ├── PROJECT_SPECIFICATION.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   ├── API_CONTRACTS.md
│   ├── DATA_SOURCES.md
│   ├── ML_EVALUATION.md
│   └── DEVELOPMENT_PLAN.md
|
├── scripts/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

Adjust this structure only when there is a documented reason.

# 16. IMPLEMENTATION PHASES AND DELIVERABLES

Implement in the following order.

## Phase 0 — Repository and architecture foundation

Deliverables:
- Monorepo.
- Architecture document.
- ER diagram.
- Database design.
- Java project skeleton.
- Python project skeleton.
- React project skeleton.
- Docker Compose.
- Environment configuration.
- Health checks.
- CI pipeline.

Acceptance:
All services start locally, expose health endpoints and communicate using documented internal contracts.

## Phase 1 — Historical data foundation

Deliverables:
- Race source adapters.
- Historical ingestion.
- Data normalization.
- Database migrations.
- Race, rider and session APIs.
- Data quality checks.
- Sample fixtures.

Acceptance:
A user can retrieve historical race results and lap-level data through Java APIs.

## Phase 2 — News intelligence

Deliverables:
- Configurable news sources.
- Java news ingestion scheduler.
- Article normalization.
- Duplicate detection.
- Python classification.
- Importance ranking.
- AI summarization.
- Story grouping.
- News APIs.
- Top five dashboard widget.
- Top ten news page.

Acceptance:
The system can retrieve and display ranked MotoGP news with accurate publication times, original links and source attribution. It must remain operational if an individual news source fails.

## Phase 3 — Historical analytics and frontend

Deliverables:
- Home dashboard.
- Historical race explorer.
- Rider profiles.
- Rider comparison.
- Lap-time charts.
- Race replay.
- Team and circuit pages.

Acceptance:
A user can explore historical races without AI services being required.

## Phase 4 — Predictive ML

Deliverables:
- Feature engineering.
- Baseline models.
- Lap-time prediction.
- Pace degradation detection.
- Race outcome prediction.
- Rider clustering.
- Chronological validation.
- MLflow integration.
- Prediction APIs.
- Model documentation.

Acceptance:
Every production model has an evaluation report, documented features, training data cutoff, version and baseline comparison.

## Phase 5 — RAG

Deliverables:
- Document ingestion.
- Chunking.
- Embedding generation.
- pgvector integration.
- Hybrid retrieval.
- Metadata filters.
- Source citations.
- Retrieval evaluation.

Acceptance:
Users can ask questions about historical MotoGP documents and receive answers with verifiable source references.

## Phase 6 — Multi-agent AI

Deliverables:
- LangGraph orchestrator.
- Race Director.
- Performance Analyst.
- Prediction Analyst.
- Research Analyst.
- Strategy Analyst.
- Structured tools.
- Shared state.
- Execution traces.
- Error handling.
- AI chat frontend.

Acceptance:
A complex user question can invoke multiple tools and agents, combine the results and return a grounded response with data and source citations.

## Phase 7 — Race simulation

Deliverables:
- Monte Carlo engine.
- Scenario configuration.
- Race outcome simulation.
- Championship simulation.
- Historical backtesting.
- Simulation API.
- Interactive frontend.

Acceptance:
Users can run repeatable simulations with documented assumptions and inspect outcome distributions.

## Phase 8 — Live intelligence

Deliverables:
- Authorized live data adapter.
- Redis live state.
- Event normalization.
- SSE endpoint.
- Live dashboard.
- Historical replay fallback.
- Feed freshness monitoring.

Acceptance:
The application displays real live information when available and clearly identifies when it is displaying recorded or stale data.

## Phase 9 — Production hardening

Deliverables:
- Complete automated test suite.
- Security review.
- Performance testing.
- Structured logging.
- Distributed tracing.
- Prometheus metrics.
- Grafana dashboards.
- CI/CD.
- Deployment documentation.
- Backup and recovery procedures.
- User-facing documentation.

Acceptance:
All core functionality is testable, deployable and documented.

# 17. DEFINITION OF DONE

A module is not complete simply because its main page renders.

Each module must include:

1. Working backend implementation.
2. Working frontend integration.
3. Database migrations.
4. Input validation.
5. Error handling.
6. Automated tests.
7. API documentation.
8. Logging and metrics where applicable.
9. Configuration documentation.
10. Demonstrable acceptance criteria.

A model is not complete simply because it produces a prediction.

It must include:
- A reproducible training pipeline.
- A documented dataset.
- A baseline.
- Chronological evaluation.
- Metrics.
- A versioned artifact.
- An inference API.
- Tests.

An AI agent is not complete simply because it responds to questions.

It must:
- Use real tools.
- Retrieve actual data.
- Handle missing information.
- Cite evidence.
- Enforce execution limits.
- Be evaluated against a test set.

A news module is not complete simply because it displays headlines.

It must:
- Have source attribution.
- Have working original links.
- Preserve timestamps.
- Deduplicate stories.
- Rank important stories.
- Identify AI-generated summaries.
- Handle unavailable sources.

# 18. CODEX IMPLEMENTATION INSTRUCTIONS

These instructions are mandatory for all implementation work.

## General rules

1. Read the entire PROJECT_SPECIFICATION.md before making changes.
2. Inspect the existing repository before creating or modifying files.
3. Reuse existing working MotoGP data ingestion, Go tracking and Java backend components where appropriate.
4. Do not overwrite existing working functionality without justification.
5. Use Java for core application implementation.
6. Use Python exclusively for AI/ML subsystem responsibilities.
7. Maintain clear Java/Python API contracts.
8. Do not duplicate domain logic between Java and Python.
9. Do not introduce technologies that are not justified by requirements.
10. Use environment variables for configurable values.
11. Never commit secrets.
12. Use database migrations for schema changes.
13. Add tests alongside implementation.
14. Update documentation when interfaces change.
15. Prefer small, independently verifiable implementation steps.

## Implementation workflow

Before each phase:

1. Read the relevant requirements.
2. Inspect dependencies and existing code.
3. Produce a short implementation plan.
4. Identify affected modules and interfaces.
5. Identify testing requirements.
6. Identify external data dependencies.
7. Implement the phase.
8. Run relevant tests.
9. Fix failures before proceeding.
10. Update documentation and the project progress tracker.

Do not implement the entire system in one generation.

Do not create placeholder implementations and mark them complete.

Do not silently skip unavailable data sources.

Do not claim an integration works unless it has been tested.

If a data provider requires credentials, document the requirement and provide a mock adapter for local development.

## Architecture governance

When changing a major architectural decision:

- Explain why the existing design is insufficient.
- Describe the alternative.
- Identify migration requirements.
- Document the decision in docs/architecture/decisions/.
- Preserve compatibility where possible.

## Required project tracking

Maintain:

- docs/IMPLEMENTATION_STATUS.md
- docs/KNOWN_LIMITATIONS.md
- docs/ARCHITECTURE_DECISIONS.md

Update IMPLEMENTATION_STATUS.md after each completed task.

Each task should have:
- Status.
- Files changed.
- Tests executed.
- Test results.
- Remaining issues.

## Initial Codex task

Do not immediately implement all modules.

Start by:

1. Inspecting the existing repository.
2. Identifying existing working MotoGP components.
3. Comparing the existing implementation against this specification.
4. Producing an architecture gap analysis.
5. Producing a detailed Phase 0 implementation plan.
6. Identifying data-source access risks.
7. Proposing the initial database ER diagram.
8. Waiting for approval before making large architectural changes.

Once Phase 0 is approved, implement it completely and validate it before proceeding.

# 19. INITIAL RELEASE SCOPE

The first complete demonstrable release must contain:

- Historical MotoGP data.
- Race and rider exploration.
- Rider comparison.
- Top five important news updates.
- Top ten news page.
- Lap-time prediction.
- Pace degradation analysis.
- RAG knowledge retrieval.
- AI Race Engineer with multiple specialized agents.
- Interactive dashboard.
- Docker-based local deployment.

Live race integration and advanced race simulation should be added after the core product works reliably.

The project should support recorded-session data so the platform remains demonstrable outside race weekends.

# 20. PROJECT SUCCESS CRITERIA

The finished project should demonstrate:

- Strong Java backend architecture.
- Python-based AI/ML integration.
- Full-stack development.
- Real-world data engineering.
- Predictive modelling with measurable evaluation.
- Multi-agent AI orchestration.
- RAG with verifiable citations.
- News aggregation and ranking.
- Asynchronous processing.
- Caching and performance engineering.
- Automated testing.
- Containerization.
- Observability.
- Production-oriented design.

The final result should be a cohesive MotoGP intelligence product, not a collection of independent demos.

**Primary objective: Build a reliable analytics platform first, an intelligent predictive system second, and an AI race engineer on top of those foundations.**