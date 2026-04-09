-- Embeddings for semantic validation (pgvector). Safe to run on existing DBs.
-- Requires extension vector (see V002_enable_pgvector.sql).
--
-- docker exec -i quizai-postgres psql -U postgres -d quizai < docker/migrations/V003_add_questions_embedding_column.sql

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE questions ADD COLUMN IF NOT EXISTS embedding vector(384);
