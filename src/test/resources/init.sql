CREATE TABLE IF NOT EXISTS topics (
    id SERIAL PRIMARY KEY,
    name TEXT
);

CREATE TABLE IF NOT EXISTS achievements (
    id SERIAL PRIMARY KEY,
    name TEXT,
    profile_pic_needed BOOLEAN,
    description_needed BOOLEAN,
    games_number_needed INT,
    global_points_needed INT,
    global_rating_place_needed INT,
    current_game_points_needed INT,
    current_game_rating_needed INT,
    current_game_level_difficulty_needed INT
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    session TEXT,
    username TEXT,
    password TEXT,
    current_game_id INT,
    pic_id INT,
    description TEXT,
    last_activity TIME,
    games_played_number INT,
    global_points INT,
    global_possible_points INT,
    games_played_ids JSON,
    achievement_ids JSON,
    current_game_points INT
);

CREATE TABLE IF NOT EXISTS games (
    id SERIAL PRIMARY KEY,
    status SMALLINT,
    author_id INT REFERENCES users (id) ON DELETE SET NULL,
    game_start_time TIMESTAMP,
    game_end_time TIMESTAMP,
    is_private BOOLEAN,
    level_difficulty SMALLINT CHECK (level_difficulty >= 1 AND level_difficulty <= 3),
    current_question_number INT,
    number_of_questions INT,
    participants_number SMALLINT CHECK (participants_number >= 4 AND participants_number <= 15),
    topic_id INT REFERENCES topics (id) ON DELETE SET NULL
);

ALTER TABLE users ADD CONSTRAINT fk_users_current_game FOREIGN KEY (current_game_id) REFERENCES games (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS questions (
    id SERIAL PRIMARY KEY,
    game_id INT REFERENCES games (id) ON DELETE CASCADE,
    question_number INT,
    question_text TEXT,
    right_answer_number SMALLINT CHECK (right_answer_number >= 1 AND right_answer_number <= 4),
    answer1 TEXT,
    answer2 TEXT,
    answer3 TEXT,
    answer4 TEXT
);

INSERT INTO games (status) VALUES (-1);
INSERT INTO topics (name) VALUES ('testTopic');
INSERT INTO achievements (name) VALUES ('testAchievement');