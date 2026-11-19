INSERT INTO providers (code, name, kind, enabled, terms_url)
VALUES ('news-rss', 'Configurable RSS and Atom News Sources', 'NEWS', true, NULL)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE source_records DROP CONSTRAINT source_records_entity_type_check;
ALTER TABLE source_records ADD CONSTRAINT source_records_entity_type_check
    CHECK (entity_type IN (
        'SEASON', 'EVENT', 'CIRCUIT', 'RIDER', 'TEAM', 'MANUFACTURER', 'SESSION',
        'TEAM_MANUFACTURER_SEASON', 'RIDER_SEASON_ENTRY',
        'SESSION_RESULT', 'RIDER_LAP', 'RACE_POSITION', 'NEWS_ARTICLE'));

CREATE TABLE news_sources (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES providers(id),
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    source_type text NOT NULL CHECK (source_type IN ('RSS', 'ATOM', 'RSS_ATOM')),
    feed_url text NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    timeout_ms integer NOT NULL DEFAULT 10000 CHECK (timeout_ms BETWEEN 1000 AND 120000),
    last_attempted_at timestamptz,
    last_successful_fetch_at timestamptz,
    last_failed_at timestamptz,
    last_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE news_stories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title text NOT NULL,
    importance_score numeric(5,2) CHECK (importance_score BETWEEN 0 AND 100),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE news_articles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id uuid REFERENCES news_stories(id),
    source_id uuid NOT NULL REFERENCES news_sources(id),
    canonical_url text NOT NULL UNIQUE,
    deduplication_key text NOT NULL UNIQUE,
    original_url text,
    title text NOT NULL,
    published_at timestamptz,
    permitted_excerpt text,
    image_url text,
    content_hash char(64),
    fetched_at timestamptz NOT NULL,
    primary_source_record_id uuid REFERENCES source_records(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_news_sources_enabled ON news_sources(enabled, updated_at);
CREATE INDEX idx_news_articles_published ON news_articles(published_at DESC NULLS LAST, id DESC);
CREATE INDEX idx_news_articles_source ON news_articles(source_id, published_at DESC NULLS LAST);
CREATE INDEX idx_news_articles_source_record ON news_articles(primary_source_record_id);
