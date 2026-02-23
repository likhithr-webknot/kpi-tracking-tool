BEGIN;

CREATE TABLE IF NOT EXISTS dev.band_directory (
  code dev.current_band PRIMARY KEY,
  label varchar(100) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE IF NOT EXISTS dev.stream_directory (
  code dev.current_stream PRIMARY KEY,
  label varchar(100) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer,
  created_at timestamp,
  updated_at timestamp
);

INSERT INTO dev.band_directory (code, label, active, sort_order, created_at, updated_at)
VALUES
  ('B1',  'Band 1',  true,  1, now(), now()),
  ('B2',  'Band 2',  true,  2, now(), now()),
  ('B3',  'Band 3',  true,  3, now(), now()),
  ('B4',  'Band 4',  true,  4, now(), now()),
  ('B5',  'Band 5',  true,  5, now(), now()),
  ('B5H', 'Band 5H', true,  6, now(), now()),
  ('B5L', 'Band 5L', true,  7, now(), now()),
  ('B6H', 'Band 6H', true,  8, now(), now()),
  ('B6L', 'Band 6L', true,  9, now(), now()),
  ('B7H', 'Band 7H', true, 10, now(), now()),
  ('B7L', 'Band 7L', true, 11, now(), now()),
  ('B8',  'Band 8',  true, 12, now(), now())
ON CONFLICT (code) DO UPDATE SET
  label = EXCLUDED.label,
  active = EXCLUDED.active,
  sort_order = EXCLUDED.sort_order,
  updated_at = now();

INSERT INTO dev.stream_directory (code, label, active, sort_order, created_at, updated_at)
VALUES
  ('Development', 'Development', true, 1, now(), now()),
  ('QA',          'QA',          true, 2, now(), now()),
  ('Devops',      'DevOps',      true, 3, now(), now()),
  ('DATA',        'Data',        true, 4, now(), now()),
  ('UI_UX',       'UI/UX',       true, 5, now(), now())
ON CONFLICT (code) DO UPDATE SET
  label = EXCLUDED.label,
  active = EXCLUDED.active,
  sort_order = EXCLUDED.sort_order,
  updated_at = now();

COMMIT;

