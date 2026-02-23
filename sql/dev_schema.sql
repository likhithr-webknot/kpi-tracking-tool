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

COMMIT;
