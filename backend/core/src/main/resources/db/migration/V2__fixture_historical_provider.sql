INSERT INTO providers (code, name, kind, enabled, terms_url)
VALUES ('fixture-historical', 'Fixture Historical Provider', 'FIXTURE', true, NULL)
ON CONFLICT (code) DO NOTHING;
