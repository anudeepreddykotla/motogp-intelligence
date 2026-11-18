# MotoGP Intelligence Platform

An AI-powered MotoGP analytics platform combining real-time race intelligence, historical performance analytics, predictive machine learning, motorsport news aggregation, and a multi-agent AI Race Engineer.

The platform is designed to turn MotoGP race data and trusted motorsport information into actionable insights for fans, analysts, and motorsport enthusiasts.

> **Project status:** Initial development — architecture and implementation planning.

## Vision

Build a full-stack motorsport intelligence platform that brings together:

- Live race monitoring and historical race exploration.
- Rider, team, manufacturer, and circuit analytics.
- Lap-time prediction and race-performance analysis.
- AI-ranked MotoGP news and important updates.
- Retrieval-Augmented Generation (RAG) for grounded motorsport research.
- A multi-agent AI Race Engineer that combines historical data, ML predictions, and retrieved information.
- Race and championship scenario simulations.

## Core Features

### 1. Race Intelligence

- Explore MotoGP seasons, events, circuits, sessions, and results.
- Analyse rider and team performance.
- Compare riders across sessions, circuits, and seasons.
- Visualise lap times, sector times, positions, and historical trends.
- Support live race monitoring where reliable data sources are available.

### 2. MotoGP News & Updates

- Display the five most important recent MotoGP updates on the dashboard.
- Provide a dedicated news page featuring the ten most important stories.
- Categorise news by riders, teams, manufacturers, race results, injuries, transfers, regulations, and technical developments.
- Generate AI-assisted summaries and extract relevant entities.
- Rank stories using importance, relevance, source credibility, and recency.
- Deduplicate syndicated coverage and link users to original publishers.

### 3. Predictive Analytics

- Lap-time prediction.
- Rider pace and performance analysis.
- Pace deterioration and anomaly detection.
- Race outcome and finishing-position predictions.
- Rider performance similarity.
- Model evaluation, versioning, and prediction history.

### 4. AI Race Engineer

A multi-agent AI system that answers motorsport questions by combining different sources of evidence.

Planned specialist agents:

- **Race Director:** Coordinates requests and delegates work.
- **Performance Analyst:** Queries structured race data and analyses performance.
- **Prediction Analyst:** Uses trained ML models to generate forecasts.
- **Research Analyst:** Retrieves relevant information through RAG.
- **Strategy Analyst:** Combines evidence and evaluates possible race scenarios.

AI-generated numerical claims must be grounded in database queries or model outputs. Research answers should include supporting sources.

### 5. Race Strategy & Simulation

- Simulate hypothetical race scenarios.
- Compare alternative race strategies.
- Run Monte Carlo championship simulations.
- Analyse uncertainty and possible outcomes.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript |
| Main backend | Java 21, Spring Boot |
| AI and ML services | Python, FastAPI |
| Agent orchestration | LangGraph |
| ML models | LightGBM / XGBoost, scikit-learn |
| Database | PostgreSQL |
| Vector search | pgvector |
| Cache and live state | Redis |
| Live frontend updates | Server-Sent Events (SSE) |
| Database migrations | Flyway |
| Containers | Docker, Docker Compose |
| Monitoring | Prometheus, Grafana, OpenTelemetry |
| Testing | JUnit 5, Mockito, pytest, Testcontainers |

The Java backend owns the main application domain, APIs, authentication, persistence, ingestion orchestration, and service integration.

Python owns AI orchestration, RAG, feature engineering, ML training, and inference.

## High-Level Architecture

```text
                    React + TypeScript
                           |
                     Spring Boot API
                           |
          +----------------+----------------+
          |                |                |
      PostgreSQL         Redis          Python AI
      + pgvector        Live State       Services
          |                                 |
    Historical Data                  +------+------+
    Race Analytics                   |      |      |
    News & Documents                 ML     RAG   Agents
    Predictions                             |
                                      LLM Provider
```

The initial architecture will use a modular Java backend and a separately deployable Python AI service. Additional infrastructure should only be introduced when justified by actual requirements.

## Data Sources & Reliability

MotoGP data availability and licensing vary by provider.

The platform will use provider adapters to isolate external data formats from the internal domain model.

Potential sources include:

- Official MotoGP publications and permitted data sources.
- Historical race-result and timing datasets.
- Permitted news feeds and licensed news APIs.
- Live timing sources where access is reliable and authorised.

Unofficial endpoints must not be treated as guaranteed production dependencies. The platform should support historical data, recorded sessions, and mock fixtures when live feeds are unavailable.

Full motorcycle telemetry must not be assumed to be publicly available.

## Repository Structure

The planned monorepo structure is:

```text
motogp-intelligence/
├── frontend/
│   └── web/
├── backend/
│   └── core/
├── ai/
│   ├── app/
│   │   ├── agents/
│   │   ├── api/
│   │   ├── ml/
│   │   ├── news/
│   │   ├── rag/
│   │   ├── simulation/
│   │   └── common/
│   ├── training/
│   ├── evaluation/
│   └── tests/
├── data-ingestion/
│   ├── historical/
│   ├── live/
│   ├── news/
│   └── documents/
├── datasets/
│   ├── raw/
│   ├── processed/
│   └── fixtures/
├── infrastructure/
│   ├── docker/
│   ├── monitoring/
│   ├── postgres/
│   └── kubernetes/
├── docs/
├── scripts/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

The actual structure may be refined during architecture planning, with significant changes documented through Architecture Decision Records (ADRs).

## Engineering Principles

- Keep business logic separate from infrastructure and external providers.
- Use typed, versioned contracts between Java and Python.
- Preserve raw source data separately from normalised and derived data.
- Use database migrations for schema changes.
- Make ingestion jobs idempotent and observable.
- Keep AI tools constrained, validated, and auditable.
- Never allow an LLM to invent race statistics or execute unrestricted SQL.
- Make historical analytics independent of external LLM availability.
- Use environment variables for configuration and secrets.
- Provide meaningful automated tests for all major workflows.
- Avoid unnecessary microservices and infrastructure complexity.

## Development Roadmap

| Phase | Deliverable |
|---|---|
| 0 | Architecture, repository structure, service skeletons, Docker Compose |
| 1 | Historical data ingestion, database schema, Java APIs |
| 2 | News ingestion, ranking, summaries, and news frontend |
| 3 | Race analytics, rider profiles, comparisons, and replay |
| 4 | ML models, evaluation pipelines, and prediction APIs |
| 5 | RAG ingestion, retrieval, and grounded research |
| 6 | Multi-agent AI Race Engineer |
| 7 | Race and championship simulation |
| 8 | Reliable live data integration and real-time updates |
| 9 | Security, observability, CI/CD, deployment, and hardening |

## Current Status

This repository is being established as a clean implementation.

No feature should be considered complete until its implementation, tests, and documentation are available.

Implementation progress, known limitations, data-source constraints, and architectural decisions will be tracked in `docs/IMPLEMENTATION_STATUS.md`, `docs/KNOWN_LIMITATIONS.md`, and `docs/ARCHITECTURE_DECISIONS.md`.

## Getting Started

Initial setup instructions will be added as the application skeleton and development environment are implemented.

The intended development environment uses Java 21, Python, Node.js, Docker, and Docker Compose.

## License

To be determined. External datasets, news content, and third-party APIs remain subject to their respective licences and terms of use.
