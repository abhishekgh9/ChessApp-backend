CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL DEFAULT 1500,
    avatar_url VARCHAR(255),
    country VARCHAR(4),
    title VARCHAR(20),
    wins_count INTEGER NOT NULL DEFAULT 0,
    losses_count INTEGER NOT NULL DEFAULT 0,
    draws_count INTEGER NOT NULL DEFAULT 0,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    move_sounds BOOLEAN NOT NULL DEFAULT TRUE,
    notification_sounds BOOLEAN NOT NULL DEFAULT TRUE,
    game_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    chat_messages BOOLEAN NOT NULL DEFAULT TRUE,
    board_theme VARCHAR(30) NOT NULL DEFAULT 'classic',
    default_time_control VARCHAR(15) NOT NULL DEFAULT '10+0',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY,
    white_player_id UUID REFERENCES users(id),
    black_player_id UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    result VARCHAR(20),
    result_reason VARCHAR(40),
    fen VARCHAR(255) NOT NULL,
    pgn TEXT NOT NULL,
    history_json TEXT NOT NULL,
    time_control VARCHAR(15) NOT NULL,
    white_time_remaining INTEGER NOT NULL,
    black_time_remaining INTEGER NOT NULL,
    turn_color VARCHAR(5) NOT NULL,
    last_move_from VARCHAR(5),
    last_move_to VARCHAR(5),
    last_move_san VARCHAR(30),
    rated BOOLEAN NOT NULL DEFAULT FALSE,
    draw_offered_by UUID,
    is_bot_game BOOLEAN NOT NULL DEFAULT FALSE,
    bot_level INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS game_moves (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id),
    player_id UUID NOT NULL REFERENCES users(id),
    move_number INTEGER NOT NULL,
    move_color VARCHAR(5) NOT NULL,
    from_square VARCHAR(5) NOT NULL,
    to_square VARCHAR(5) NOT NULL,
    promotion VARCHAR(5),
    san VARCHAR(30) NOT NULL,
    fen_after VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS game_chat_messages (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS game_analyses (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL UNIQUE REFERENCES games(id),
    game_status VARCHAR(20) NOT NULL,
    game_result VARCHAR(20),
    overall_accuracy DOUBLE PRECISION,
    white_player_id UUID REFERENCES users(id),
    white_accuracy DOUBLE PRECISION,
    white_current_rating INTEGER,
    white_provisional_rating INTEGER,
    white_rating_delta INTEGER,
    white_moves_analyzed INTEGER,
    black_player_id UUID REFERENCES users(id),
    black_accuracy DOUBLE PRECISION,
    black_current_rating INTEGER,
    black_provisional_rating INTEGER,
    black_rating_delta INTEGER,
    black_moves_analyzed INTEGER,
    source_game_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_move_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS game_analysis_moves (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES game_analyses(id),
    player_id UUID REFERENCES users(id),
    move_number INTEGER NOT NULL,
    move_color VARCHAR(5) NOT NULL,
    uci_move VARCHAR(5) NOT NULL,
    best_move VARCHAR(5),
    evaluation_after DOUBLE PRECISION,
    classification VARCHAR(20) NOT NULL,
    accuracy DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS achievements (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_achievements (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    achievement_id UUID NOT NULL REFERENCES achievements(id),
    earned BOOLEAN NOT NULL DEFAULT TRUE,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS fide_players (
    fide_id INTEGER PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    title VARCHAR(20),
    federation VARCHAR(4),
    sex VARCHAR(1),
    birth_year INTEGER,
    standard_rating INTEGER,
    rapid_rating INTEGER,
    blitz_rating INTEGER,
    standard_games INTEGER,
    rapid_games INTEGER,
    blitz_games INTEGER,
    standard_k INTEGER,
    rapid_k INTEGER,
    blitz_k INTEGER,
    standard_inactive BOOLEAN NOT NULL DEFAULT FALSE,
    rapid_inactive BOOLEAN NOT NULL DEFAULT FALSE,
    blitz_inactive BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS puzzles (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    fen VARCHAR(255) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    primary_theme VARCHAR(40) NOT NULL,
    max_wrong_attempts INTEGER NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS puzzle_solution_steps (
    id UUID PRIMARY KEY,
    puzzle_id UUID NOT NULL REFERENCES puzzles(id),
    step_number INTEGER NOT NULL,
    move_uci VARCHAR(5) NOT NULL,
    move_san VARCHAR(30),
    side_to_move VARCHAR(5) NOT NULL,
    is_opponent_move BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT puzzle_solution_steps_unique_step UNIQUE (puzzle_id, step_number)
);

CREATE TABLE IF NOT EXISTS puzzle_tags (
    id UUID PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    slug VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS puzzle_tag_links (
    puzzle_id UUID NOT NULL REFERENCES puzzles(id),
    tag_id UUID NOT NULL REFERENCES puzzle_tags(id),
    PRIMARY KEY (puzzle_id, tag_id)
);

CREATE TABLE IF NOT EXISTS puzzle_attempts (
    id UUID PRIMARY KEY,
    puzzle_id UUID NOT NULL REFERENCES puzzles(id),
    user_id UUID NOT NULL REFERENCES users(id),
    attempt_number INTEGER NOT NULL,
    solution_step_number INTEGER NOT NULL,
    submitted_move_uci VARCHAR(5) NOT NULL,
    status VARCHAR(20) NOT NULL,
    time_spent_seconds INTEGER NOT NULL DEFAULT 0,
    hints_used INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fide_players_standard_rating ON fide_players (standard_rating DESC);
CREATE INDEX IF NOT EXISTS idx_fide_players_rapid_rating ON fide_players (rapid_rating DESC);
CREATE INDEX IF NOT EXISTS idx_fide_players_blitz_rating ON fide_players (blitz_rating DESC);
CREATE INDEX IF NOT EXISTS idx_fide_players_federation ON fide_players (federation);
CREATE INDEX IF NOT EXISTS idx_puzzles_difficulty_theme ON puzzles (difficulty, primary_theme);
CREATE INDEX IF NOT EXISTS idx_puzzles_active_created_at ON puzzles (active, created_at, id);
CREATE INDEX IF NOT EXISTS idx_puzzle_solution_steps_puzzle_step ON puzzle_solution_steps (puzzle_id, step_number);
CREATE INDEX IF NOT EXISTS idx_puzzle_attempts_user_created_at ON puzzle_attempts (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_puzzle_attempts_puzzle_user ON puzzle_attempts (puzzle_id, user_id);
CREATE INDEX IF NOT EXISTS idx_puzzle_attempts_status ON puzzle_attempts (status);
