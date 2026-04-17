-- Flyway Migration: Initial Database Setup
-- This migration is a placeholder as schema will be created by Hibernate ORM (ddl-auto=update)
-- The following tables will be auto-created by Hibernate:
-- - users
-- - job
-- - job_history
-- - dead_letter_job
-- - gmail_token
-- - job_template (NEW)
-- - notification (NEW)

-- Add created_at column to job table if it doesn't exist
ALTER TABLE job ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
