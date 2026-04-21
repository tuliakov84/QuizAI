CREATE TABLE IF NOT EXISTS answered_correctly_questions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users (id) ON DELETE CASCADE,
    question_id INT REFERENCES questions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS answered_correctly_questions_idx
    ON answered_correctly_questions (user_id);
