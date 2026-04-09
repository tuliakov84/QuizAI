-- Backfill for existing DBs where answered_correctly_questions table is missing.
-- Safe to run multiple times.
--
-- docker exec -i quizai-postgres psql -U postgres -d quizai < docker/migrations/V004_create_answered_correctly_questions.sql

CREATE TABLE IF NOT EXISTS answered_correctly_questions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users (id) ON DELETE CASCADE,
    question_id INT REFERENCES questions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS answered_correctly_questions_idx
    ON answered_correctly_questions (user_id);
