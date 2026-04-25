-- V6: Zero-downtime step 3 - enforce NOT NULL now that all rows are backfilled.
--
-- This is safe because V5 guaranteed no NULL values remain.
-- The 3-step pattern (nullable → backfill → NOT NULL) avoids locking the
-- table in production: each migration is small, fast, and independently safe.
ALTER TABLE employees ALTER COLUMN department_id BIGINT NOT NULL;
