BEGIN;

INSERT INTO dev.certifications (name, active, created_at, updated_at)
VALUES
  ('AWS Certified Developer - Associate', true, now(), now()),
  ('AWS Certified Solutions Architect - Associate', true, now(), now()),
  ('Microsoft Certified: Azure Developer Associate', true, now(), now()),
  ('Google Professional Cloud Architect', true, now(), now()),
  ('Certified Kubernetes Administrator (CKA)', true, now(), now()),
  ('Docker Certified Associate', true, now(), now()),
  ('ISTQB Certified Tester Foundation Level (CTFL)', true, now(), now()),
  ('Certified ScrumMaster (CSM)', true, now(), now()),
  ('PMP - Project Management Professional', true, now(), now()),
  ('Oracle Certified Professional, Java SE', true, now(), now())
ON CONFLICT (name) DO UPDATE SET
  active = EXCLUDED.active,
  updated_at = now();

INSERT INTO dev.webknot_values (title, pillar, description, active, created_at, updated_at)
VALUES
  (
    'Customer First',
    'Client Excellence',
    'Understand customer goals deeply and deliver outcomes that create measurable business value.',
    true,
    now(),
    now()
  ),
  (
    'Ownership',
    'Accountability',
    'Take end-to-end responsibility for commitments, quality, timelines, and production stability.',
    true,
    now(),
    now()
  ),
  (
    'Quality by Default',
    'Engineering Excellence',
    'Build secure, reliable, and maintainable solutions with strong testing and review discipline.',
    true,
    now(),
    now()
  ),
  (
    'Continuous Learning',
    'Growth Mindset',
    'Continuously improve technical and functional capability through practice, feedback, and mentoring.',
    true,
    now(),
    now()
  ),
  (
    'Collaboration',
    'Teamwork',
    'Work effectively across functions with clear communication, mutual respect, and shared goals.',
    true,
    now(),
    now()
  ),
  (
    'Innovation',
    'Future Readiness',
    'Explore new tools, architecture patterns, and process improvements to drive long-term value.',
    true,
    now(),
    now()
  ),
  (
    'Integrity',
    'Professional Conduct',
    'Act transparently, ethically, and consistently in all decisions and stakeholder interactions.',
    true,
    now(),
    now()
  ),
  (
    'Delivery Discipline',
    'Execution',
    'Plan realistically, track diligently, and deliver predictable outcomes without compromising quality.',
    true,
    now(),
    now()
  )
ON CONFLICT (title) DO UPDATE SET
  pillar = EXCLUDED.pillar,
  description = EXCLUDED.description,
  active = EXCLUDED.active,
  updated_at = now();

COMMIT;
