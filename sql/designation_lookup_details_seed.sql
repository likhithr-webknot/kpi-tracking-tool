BEGIN;

ALTER TABLE dev.designation_lookup
  ADD COLUMN IF NOT EXISTS designation_titles text,
  ADD COLUMN IF NOT EXISTS time_period varchar(50),
  ADD COLUMN IF NOT EXISTS responsibilities text;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B4'::dev.current_band,
  'Senior Technical Architect / Senior Delivery Manager',
  'Senior Technical Architect / Senior Delivery Manager',
  '3-5 years',
  $$Technology solutioning and delivery success is the primary responsibility, ensuring all solutions are scalable, secure, and aligned with business goals.

Owns technology decisions and solution choices across multiple projects and ensures consistency, quality standards, and architectural alignment.

Demonstrates capability to manage large-scale live production systems and designs resilient, performance-driven solutions.

Leads and owns the LevelUp program for the assigned technology track (Backend / Frontend / Mobile / DevOps), including roadmap, execution, and outcome tracking.

Influences internal and external stakeholders on technology decisions through strong articulation and technical leadership.

Drives organization-wide contribution: technology initiatives, internal training, knowledge sharing, and engineering culture.

Partners with Talent and HR to execute technical programs, events, learning initiatives, and workforce capability building.

Expected to operate as a technology leader for the organization with ownership of solution quality, mentoring, decision influence, and long-term capability building.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B5'::dev.current_band,
  'Principal Engineer / Delivery Manager',
  $$Principal Engineer
QA Lead
UI/UX Lead
Delivery Manager
AM Lead
Senior HR Manager
Senior Finance Manager$$,
  '3-4 years',
  $$Technology Leadership:
- Demonstrates visible technology leadership and contributes to strategic technical direction.
- Participates in architecture reviews and organization-wide technical forums.
- Publishes technical blogs, knowledge articles, and internal sessions.
- Owns technical delivery for one or more teams and can manage additional client delivery engagement when applicable.
- Drives PoCs for new technologies and capabilities.
- Leads scalable, secure, high-performance cloud architecture.
- Leads complex technical discussions with stakeholders and drives aligned decisions.
- Establishes engineering practices, standards, and processes across streams.
- Identifies strategic technologies and builds internal expertise.
- Owns technology leadership in pre-sales and one LevelUp track.
- Owns architecture, roadmap, and governance of at least one in-house product.

Delivery and Management Leadership:
- Acts as end-to-end delivery owner for projects.
- Maintains client relationships supporting account growth.
- Acts as escalation point during crises and leads resolution.
- Owns delivery outcomes, risks, failures, and corrective actions.
- Designs MSP delivery execution plans.
- Maintains standardized project/product process documentation.
- Identifies role-based certifications and training.
- Builds structured learning for PM/APM growth.
- Introduces delivery and management practices for better execution.
- Owns pre-sales delivery planning, resourcing, and timelines.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B6H'::dev.current_band,
  'Sr. Team Lead / Product Owner / Project Manager',
  $$Sr. Team Lead - Developer Captain
Product Owner - Management Captain
Project Manager$$,
  '1-3 years',
  $$Core Responsibilities:
- Owns assigned products/projects from initiation through closure.
- Coordinates Development, QA, DevOps, and Support teams for smooth execution and handover.
- Captures customer feedback and converts it into backlog improvements.
- Drives forecasting and reports status and risks to senior management.
- Identifies and mitigates delivery and operational risks.
- Plans and manages resource allocation and utilization.
- Supports team learning and mentoring.
- Builds strong stakeholder rapport and sustains high-performance delivery culture.

Additional role mapping from B6 track:
Senior QA, Senior Designer, Senior PM, Business Analyst, Senior AM, HR Manager, Finance Manager.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B6L'::dev.current_band,
  'Team Lead / AI Team Lead / DevOps Team Lead',
  $$Team Lead
AI Team Lead
DevOps Team Lead$$,
  '1-3 years',
  $$Technical Responsibilities:
- Leads technical execution of assigned projects/modules with timeline and quality ownership.
- Plans, assigns, and tracks technical tasks ensuring accountability.
- Enforces coding standards, architecture guidelines, and design principles.
- Mentors team members for strong outcomes.
- Participates in customer technical discussions for requirement alignment.
- Elicits and documents business/technical requirements to reduce rework.
- Drives continuous engineering workflow improvements.
- Provides technical/analytical insights for decisions and actions.

Non-Technical Responsibilities:
- Owns backlog, delivery plans, and timelines for predictable execution.
- Maintains active communication with customers and stakeholders.
- Drives milestone achievement and quality releases.
- Incorporates customer feedback into planning and execution.
- Identifies delivery/operational/dependency risks and mitigates proactively.
- Plans team capacity and utilization.

Additional role mapping from B6 track:
Senior QA, Senior Designer, Senior PM, Business Analyst, Senior AM, HR Manager, Finance Manager.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B7H'::dev.current_band,
  'Senior Software Engineer',
  $$Senior Software Engineer
Senior AI/ML Engineer
Senior DevOps Engineer
Associate QA
Associate Designer
Project Manager
Associate BA
Account Manager
HR Generalist II
Finance Associate$$,
  '1.5-3 years',
  $$Senior Software Engineer Responsibilities:
- Delivers assigned tasks within timelines with quality and correctness.
- Applies SDLC processes effectively during execution.
- Contributes to skill development and internal knowledge sharing.
- Shares clear status reports and technical documentation.
- Supports junior associates and interns.
- Follows organizational policies and engineering standards.
- Contributes to successful module/project releases through strong coordination.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B7L'::dev.current_band,
  'Software Engineer',
  $$Software Engineer
AI/ML Engineer
DevOps Engineer
Assistant QA
Assistant Designer
Assistant Project Manager
AM Assistant
HR Generalist I$$,
  '1-2 years',
  $$Technical Responsibilities:
- Contributes effectively to client and internal initiatives with quality delivery.
- Estimates and manages assigned tasks independently with predictable delivery.
- Develops clean, maintainable, tested code and supports interns.
- Demonstrates SDLC/deployment understanding including CI/CD and Git.
- Owns assigned responsibilities and timelines.
- Participates in organization training as learner and contributor.
- Contributes to in-house products/platforms when bandwidth permits.

Management / Business Analysis Responsibilities:
- Understands business requirements and raises clarifications proactively.
- Maintains SRS and functional documentation and supports estimations.
- Supports PM in sprint ceremonies, coordination, and reporting.
- Demonstrates strong written and verbal communication.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

WITH streams(stream) AS (
  VALUES ('Development'), ('QA'), ('Devops'), ('DATA'), ('UI_UX')
)
INSERT INTO dev.designation_lookup (
  stream, band, designation, designation_titles, time_period, responsibilities
)
SELECT
  s.stream,
  'B8'::dev.current_band,
  'Intern',
  $$Software Development Intern
AI/ML Engineer Intern
DevOps Engineer Intern
QA Intern
Designer Intern
Project Management Intern
BA Intern
AM Intern
HR Intern
Finance Intern$$,
  '6 months - 1 year',
  $$Intern Responsibilities:
- Learns new technologies, tools, and delivery processes quickly.
- Demonstrates a positive and professional attitude toward learning, collaboration, and growth.
- Builds technical and soft skills through projects and training.
- Takes ownership of assigned tasks and completes them accurately under guidance.
- Collaborates across teams and communicates progress/challenges clearly.
- Demonstrates cultural alignment with Webknot values and contributes positively to workplace initiatives.$$ 
FROM streams s
ON CONFLICT (stream, band) DO UPDATE SET
  designation = EXCLUDED.designation,
  designation_titles = EXCLUDED.designation_titles,
  time_period = EXCLUDED.time_period,
  responsibilities = EXCLUDED.responsibilities;

COMMIT;
