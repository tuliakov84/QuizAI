ALTER TABLE users
    ADD COLUMN IF NOT EXISTS custom_avatar_path VARCHAR(512);
