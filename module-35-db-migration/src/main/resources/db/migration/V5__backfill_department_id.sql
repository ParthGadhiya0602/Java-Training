-- V5: Zero-downtime step 2 - backfill existing rows with a default department.
--
-- All existing employees are assigned to Engineering (id = 1).
-- After this migration every row has a non-null department_id,
-- making it safe to add the NOT NULL constraint in V6.
UPDATE employees SET department_id = 1 WHERE department_id IS NULL;
