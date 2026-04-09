-- Required for questions.embedding VECTOR(...) and Python UPDATE ... SET embedding = %s::vector
-- Run once if you see: type "vector" does not exist
--
-- docker exec -i quizai-postgres psql -U postgres -d quizai < docker/migrations/V002_enable_pgvector.sql

CREATE EXTENSION IF NOT EXISTS vector;
