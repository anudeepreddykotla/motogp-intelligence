CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE providers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    kind text NOT NULL CHECK (kind IN ('RACE_DATA', 'NEWS', 'DOCUMENT', 'FIXTURE')),
    enabled boolean NOT NULL DEFAULT true,
    terms_url text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ingestion_jobs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES providers(id),
    job_type text NOT NULL CHECK (job_type IN ('REFERENCE', 'HISTORICAL', 'RECORDED', 'NEWS', 'DOCUMENT')),
    schedule text,
    enabled boolean NOT NULL DEFAULT true,
    configuration jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider_id, job_type)
);

CREATE TABLE ingestion_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id uuid NOT NULL REFERENCES ingestion_jobs(id),
    started_at timestamptz NOT NULL,
    finished_at timestamptz,
    status text NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
    records_seen integer NOT NULL DEFAULT 0 CHECK (records_seen >= 0),
    records_written integer NOT NULL DEFAULT 0 CHECK (records_written >= 0),
    error_summary text,
    CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE TABLE raw_payloads (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id uuid NOT NULL REFERENCES ingestion_attempts(id),
    provider_external_id text,
    payload_format text NOT NULL CHECK (payload_format IN ('JSON', 'XML', 'CSV', 'TEXT', 'BINARY')),
    payload_json jsonb,
    payload_uri text,
    content_sha256 char(64) NOT NULL,
    content_type text,
    captured_at timestamptz NOT NULL,
    CHECK ((payload_json IS NULL) <> (payload_uri IS NULL)),
    UNIQUE (attempt_id, content_sha256)
);

CREATE TABLE source_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES providers(id),
    raw_payload_id uuid REFERENCES raw_payloads(id),
    entity_type text NOT NULL CHECK (entity_type IN ('SEASON', 'EVENT', 'CIRCUIT', 'RIDER', 'TEAM', 'MANUFACTURER', 'SESSION')),
    provider_external_id text NOT NULL,
    source_version text,
    first_seen_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    UNIQUE (provider_id, entity_type, provider_external_id),
    CHECK (last_seen_at >= first_seen_at)
);

CREATE TABLE provider_identity_mappings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES providers(id),
    entity_type text NOT NULL CHECK (entity_type IN ('SEASON', 'EVENT', 'CIRCUIT', 'RIDER', 'TEAM', 'MANUFACTURER', 'SESSION')),
    provider_external_id text NOT NULL,
    internal_id uuid NOT NULL,
    first_seen_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    UNIQUE (provider_id, entity_type, provider_external_id),
    UNIQUE (provider_id, entity_type, internal_id),
    CHECK (last_seen_at >= first_seen_at)
);

CREATE TABLE seasons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_source_record_id uuid REFERENCES source_records(id),
    year smallint NOT NULL CHECK (year BETWEEN 1949 AND 2100),
    championship text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (year, championship)
);

CREATE TABLE circuits (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_source_record_id uuid REFERENCES source_records(id),
    name text NOT NULL,
    country_code char(2),
    timezone text NOT NULL,
    latitude numeric(8,5),
    longitude numeric(8,5),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (name, country_code),
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE riders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_source_record_id uuid REFERENCES source_records(id),
    display_name text NOT NULL,
    given_name text,
    family_name text,
    nationality_code char(2),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (display_name)
);

CREATE TABLE manufacturers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_source_record_id uuid REFERENCES source_records(id),
    name text NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_source_record_id uuid REFERENCES source_records(id),
    name text NOT NULL,
    country_code char(2),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (name, country_code)
);

CREATE TABLE events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id uuid NOT NULL REFERENCES seasons(id),
    circuit_id uuid NOT NULL REFERENCES circuits(id),
    primary_source_record_id uuid REFERENCES source_records(id),
    name text NOT NULL,
    round_number smallint NOT NULL CHECK (round_number > 0),
    scheduled_start_at timestamptz,
    timezone text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (season_id, round_number),
    UNIQUE (season_id, name)
);

CREATE TABLE team_manufacturer_seasons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id uuid NOT NULL REFERENCES seasons(id),
    team_id uuid NOT NULL REFERENCES teams(id),
    manufacturer_id uuid NOT NULL REFERENCES manufacturers(id),
    valid_from_event_id uuid REFERENCES events(id),
    valid_to_event_id uuid REFERENCES events(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (season_id, team_id, manufacturer_id, valid_from_event_id)
);

CREATE TABLE rider_season_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id uuid NOT NULL REFERENCES seasons(id),
    rider_id uuid NOT NULL REFERENCES riders(id),
    team_id uuid NOT NULL REFERENCES teams(id),
    manufacturer_id uuid REFERENCES manufacturers(id),
    competition_class text NOT NULL,
    racing_number smallint CHECK (racing_number IS NULL OR racing_number > 0),
    valid_from_event_id uuid REFERENCES events(id),
    valid_to_event_id uuid REFERENCES events(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (season_id, rider_id, valid_from_event_id)
);

CREATE TABLE sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL REFERENCES events(id),
    primary_source_record_id uuid REFERENCES source_records(id),
    session_type text NOT NULL CHECK (session_type IN ('PRACTICE', 'QUALIFYING', 'SPRINT', 'RACE', 'WARMUP', 'OTHER')),
    scheduled_start_at timestamptz,
    started_at timestamptz,
    ended_at timestamptz,
    data_mode text NOT NULL CHECK (data_mode IN ('HISTORICAL', 'RECORDED', 'LIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (event_id, session_type),
    CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_provider_identity_lookup ON provider_identity_mappings(provider_id, entity_type, provider_external_id);
CREATE INDEX idx_ingestion_attempts_job_started ON ingestion_attempts(job_id, started_at DESC);
CREATE INDEX idx_raw_payloads_hash ON raw_payloads(content_sha256);
CREATE INDEX idx_source_records_lookup ON source_records(provider_id, entity_type, provider_external_id);
CREATE INDEX idx_events_season_round ON events(season_id, round_number);
CREATE INDEX idx_sessions_event_type ON sessions(event_id, session_type);
CREATE INDEX idx_seasons_source ON seasons(primary_source_record_id);
CREATE INDEX idx_events_source ON events(primary_source_record_id);
CREATE INDEX idx_riders_source ON riders(primary_source_record_id);
