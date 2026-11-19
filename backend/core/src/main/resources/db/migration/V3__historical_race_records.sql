ALTER TABLE source_records DROP CONSTRAINT source_records_entity_type_check;
ALTER TABLE source_records ADD CONSTRAINT source_records_entity_type_check
    CHECK (entity_type IN (
        'SEASON', 'EVENT', 'CIRCUIT', 'RIDER', 'TEAM', 'MANUFACTURER', 'SESSION',
        'TEAM_MANUFACTURER_SEASON', 'RIDER_SEASON_ENTRY',
        'SESSION_RESULT', 'RIDER_LAP', 'RACE_POSITION'));

ALTER TABLE team_manufacturer_seasons
    ADD COLUMN primary_source_record_id uuid REFERENCES source_records(id);
ALTER TABLE rider_season_entries
    ADD COLUMN primary_source_record_id uuid REFERENCES source_records(id);

CREATE INDEX idx_team_manufacturer_seasons_season_team
    ON team_manufacturer_seasons(season_id, team_id);
CREATE INDEX idx_rider_season_entries_season_rider
    ON rider_season_entries(season_id, rider_id);

CREATE TABLE session_results (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES sessions(id),
    rider_id uuid NOT NULL REFERENCES riders(id),
    team_id uuid REFERENCES teams(id),
    manufacturer_id uuid REFERENCES manufacturers(id),
    grid_position smallint CHECK (grid_position IS NULL OR grid_position > 0),
    classification_position smallint CHECK (classification_position IS NULL OR classification_position > 0),
    classification_status text NOT NULL CHECK (classification_status IN ('CLASSIFIED', 'DNF', 'DNS', 'DSQ', 'OTHER')),
    laps_completed smallint CHECK (laps_completed IS NULL OR laps_completed >= 0),
    total_time_ms integer CHECK (total_time_ms IS NULL OR total_time_ms > 0),
    points numeric(6,2) CHECK (points IS NULL OR points >= 0),
    primary_source_record_id uuid REFERENCES source_records(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (session_id, rider_id)
);

CREATE TABLE rider_laps (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_result_id uuid NOT NULL REFERENCES session_results(id),
    lap_number smallint NOT NULL CHECK (lap_number > 0),
    lap_time_ms integer CHECK (lap_time_ms IS NULL OR lap_time_ms > 0),
    is_valid boolean,
    observed_at timestamptz,
    primary_source_record_id uuid REFERENCES source_records(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (session_result_id, lap_number)
);

CREATE TABLE race_positions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_result_id uuid NOT NULL REFERENCES session_results(id),
    lap_number smallint NOT NULL CHECK (lap_number >= 0),
    position smallint CHECK (position IS NULL OR position > 0),
    observed_at timestamptz NOT NULL,
    primary_source_record_id uuid REFERENCES source_records(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (session_result_id, lap_number)
);

CREATE INDEX idx_session_results_session_classification
    ON session_results(session_id, classification_position);
CREATE INDEX idx_session_results_rider ON session_results(rider_id, session_id);
CREATE INDEX idx_rider_laps_result_lap ON rider_laps(session_result_id, lap_number);
CREATE INDEX idx_race_positions_result_lap ON race_positions(session_result_id, lap_number);
