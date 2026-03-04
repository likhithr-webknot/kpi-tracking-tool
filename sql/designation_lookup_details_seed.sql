BEGIN;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B4'::dev.current_band,
  'Senior Technical Architect / Senior Delivery Manager'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B5'::dev.current_band,
  'Principal Engineer / Delivery Manager'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B6H'::dev.current_band,
  'Sr. Team Lead / Product Owner / Project Manager'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B6L'::dev.current_band,
  'Team Lead / AI Team Lead / DevOps Team Lead'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B7H'::dev.current_band,
  'Senior Software Engineer'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B7L'::dev.current_band,
  'Software Engineer'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation
)
SELECT
  s.stream,
  'B8'::dev.current_band,
  'Intern'
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation;

COMMIT;
