-- Run once on existing databases (new installs get this from init.sql):
-- docker exec -i quizai-postgres psql -U postgres -d quizai -f /path/to/this/file
-- Or from host:
-- docker exec -i quizai-postgres psql -U postgres -d quizai < docker/migrations/V001_add_questions_validated.sql

ALTER TABLE games ADD COLUMN IF NOT EXISTS questions_validated BOOLEAN NOT NULL DEFAULT FALSE;
