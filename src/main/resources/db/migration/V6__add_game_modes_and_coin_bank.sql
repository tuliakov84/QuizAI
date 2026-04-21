ALTER TABLE users
    ADD COLUMN IF NOT EXISTS coin_balance INT NOT NULL DEFAULT 100;

ALTER TABLE games
    ADD COLUMN IF NOT EXISTS game_mode VARCHAR(32) NOT NULL DEFAULT 'CASUAL';

CREATE TABLE IF NOT EXISTS coin_transactions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users (id) ON DELETE CASCADE,
    game_id INT REFERENCES games (id) ON DELETE SET NULL,
    transaction_type VARCHAR(64) NOT NULL,
    amount_delta INT NOT NULL,
    balance_after INT NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS coin_transactions_user_idx
    ON coin_transactions (user_id);
