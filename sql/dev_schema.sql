-- Postgres DDL for dev schema (generated from Java model classes)
BEGIN;

-- create schema
CREATE SCHEMA IF NOT EXISTS dev;

-- Enum types used by the application
CREATE TYPE dev.current_band AS ENUM (
  'B8','B7L','B7H','B6L','B6H','B5L','B5','B5H','B4','B3','B2','B1'
);

CREATE TYPE dev.current_stream AS ENUM (
  'Development','QA','Devops','DATA','UI_UX'
);

CREATE TYPE dev.employee_role AS ENUM (
  'Employee','Manager','Admin'
);

-- Band directory (admin managed)
CREATE TABLE IF NOT EXISTS dev.band_directory (
  code dev.current_band PRIMARY KEY,
  label varchar(100) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer,
  created_at timestamp,
  updated_at timestamp
);

-- Stream directory (admin managed)
CREATE TABLE IF NOT EXISTS dev.stream_directory (
  code dev.current_stream PRIMARY KEY,
  label varchar(100) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer,
  created_at timestamp,
  updated_at timestamp
);

-- Employees table
CREATE TABLE IF NOT EXISTS dev.employees (
  employee_id varchar(255) PRIMARY KEY,
  employee_name varchar(255),
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  emp_role dev.employee_role NOT NULL DEFAULT 'Employee',
  stream varchar(255) NOT NULL,
  band dev.current_band NOT NULL DEFAULT 'B8',
  manager_id varchar(255),
  updated_by varchar(255),
  created_at timestamp,
  updated_at timestamp,
  CONSTRAINT fk_manager FOREIGN KEY (manager_id) REFERENCES dev.employees(employee_id) ON DELETE SET NULL,
  CONSTRAINT fk_updated_by_emp FOREIGN KEY (updated_by) REFERENCES dev.employees(employee_id) ON DELETE SET NULL
);

-- Designation lookup with composite primary key (stream, band)
CREATE TABLE IF NOT EXISTS dev.designation_lookup (
  stream varchar(50) NOT NULL,
  band dev.current_band NOT NULL,
  designation varchar(100),
  PRIMARY KEY (stream, band)
);

-- KPI definitions
CREATE TABLE IF NOT EXISTS dev.kpi_definitions (
  id BIGSERIAL PRIMARY KEY,
  band dev.current_band NOT NULL,
  stream dev.current_stream NOT NULL,
  kpi_name varchar(255) NOT NULL,
  weightage numeric(5,2) NOT NULL,
  description text,
  created_at timestamp,
  CONSTRAINT kpi_definitions_band_stream_kpi_name_key UNIQUE (band, stream, kpi_name)
);

-- Certifications directory
CREATE TABLE IF NOT EXISTS dev.certifications (
  id BIGSERIAL PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  active boolean NOT NULL DEFAULT true,
  created_at timestamp,
  updated_at timestamp
);

-- Webknot values directory
CREATE TABLE IF NOT EXISTS dev.webknot_values (
  id BIGSERIAL PRIMARY KEY,
  title varchar(255) NOT NULL UNIQUE,
  pillar varchar(255),
  description text,
  active boolean NOT NULL DEFAULT true,
  created_at timestamp,
  updated_at timestamp
);

-- AI agents configuration
CREATE TABLE IF NOT EXISTS dev.ai_agents (
  id BIGSERIAL PRIMARY KEY,
  provider varchar(100) NOT NULL,
  api_key text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamp,
  updated_at timestamp,
  CONSTRAINT ai_agents_provider_api_key_key UNIQUE (provider, api_key)
);

-- Notification center (admin + manager alerts)
CREATE TABLE IF NOT EXISTS dev.notifications (
  id BIGSERIAL PRIMARY KEY,
  recipient_employee_id varchar(255) NOT NULL,
  type varchar(128) NOT NULL,
  title varchar(255),
  message text,
  payload_json text,
  is_read boolean NOT NULL DEFAULT false,
  read_at timestamp,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp,
  CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_employee_id)
    REFERENCES dev.employees(employee_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
  ON dev.notifications(recipient_employee_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read_created
  ON dev.notifications(recipient_employee_id, is_read, created_at DESC);

-- Monthly submissions workflow
CREATE TABLE IF NOT EXISTS dev.monthly_submissions (
  id BIGSERIAL PRIMARY KEY,
  employee_id varchar(255) NOT NULL,
  month varchar(7) NOT NULL,
  submission_type varchar(64) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  review_status varchar(64),
  payload_json text NOT NULL,
  manager_review_json text,
  admin_review_json text,
  submitted_at timestamp,
  manager_submitted_at timestamp,
  admin_submitted_at timestamp,
  created_at timestamp,
  updated_at timestamp,
  CONSTRAINT monthly_submissions_employee_month_type_key UNIQUE (employee_id, month, submission_type),
  CONSTRAINT fk_monthly_submission_employee FOREIGN KEY (employee_id) REFERENCES dev.employees(employee_id) ON DELETE CASCADE
);

-- Employee-level submission window override
CREATE TABLE IF NOT EXISTS dev.employee_submission_window_overrides (
  employee_id varchar(255) PRIMARY KEY,
  force_open boolean NOT NULL DEFAULT false,
  force_closed boolean NOT NULL DEFAULT false,
  updated_at timestamptz NOT NULL,
  updated_by varchar(255),
  CONSTRAINT fk_submission_override_employee FOREIGN KEY (employee_id) REFERENCES dev.employees(employee_id) ON DELETE CASCADE,
  CONSTRAINT fk_submission_override_updated_by FOREIGN KEY (updated_by) REFERENCES dev.employees(employee_id) ON DELETE SET NULL
);

-- Submission cycles
CREATE TABLE IF NOT EXISTS dev.submission_cycles (
  id uuid PRIMARY KEY,
  cycle_key varchar(7) NOT NULL UNIQUE,
  timezone varchar(64) NOT NULL,
  window_start_at timestamptz NOT NULL,
  window_end_at timestamptz,
  manual_closed boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  updated_by varchar(255),
  CONSTRAINT fk_submission_updated_by FOREIGN KEY (updated_by) REFERENCES dev.employees(employee_id) ON DELETE SET NULL
);

-- Seed band directory (safe upsert)
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

-- Seed stream directory (safe upsert)
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
