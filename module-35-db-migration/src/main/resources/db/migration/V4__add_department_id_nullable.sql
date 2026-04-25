-- V4: Zero-downtime step 1 - add the FK column as NULLABLE.
--
-- Why nullable first?
--   Existing rows already have no department assignment.
--   Adding NOT NULL here would fail immediately because the existing
--   rows cannot satisfy the constraint until they are backfilled (V5).
--   Both old and new application code can run against this schema:
--     old code ignores the new column, new code writes to it.
ALTER TABLE employees ADD COLUMN department_id BIGINT;

ALTER TABLE employees
    ADD CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES departments (id);
