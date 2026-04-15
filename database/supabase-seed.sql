-- ChessMaster Pro demo seed data for Supabase
-- Run this after database/supabase-init.sql
-- Seeded login for demo users:
-- email: tacticaltiger@chessmaster.dev
-- password: password
--
-- Other seeded users use the same password.

begin;

insert into public.users (
    id, username, email, password_hash, rating, avatar_url, country, title,
    wins_count, losses_count, draws_count, joined_at, updated_at
) values
    ('11111111-1111-1111-1111-111111111111', 'TacticalTiger', 'tacticaltiger@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1825, 'https://api.dicebear.com/7.x/identicon/svg?seed=TacticalTiger', 'IN', 'CM', 32, 11, 4, '2025-11-10T09:00:00Z', '2026-03-27T09:30:00Z'),
    ('22222222-2222-2222-2222-222222222222', 'MagnusMock', 'magnusmock@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2410, 'https://api.dicebear.com/7.x/identicon/svg?seed=MagnusMock', 'NO', 'GM', 120, 18, 9, '2025-06-14T10:00:00Z', '2026-03-27T08:45:00Z'),
    ('33333333-3333-3333-3333-333333333333', 'EndgameEva', 'endgameeva@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1988, 'https://api.dicebear.com/7.x/identicon/svg?seed=EndgameEva', 'DE', 'WIM', 58, 19, 12, '2025-08-01T12:00:00Z', '2026-03-26T21:15:00Z'),
    ('44444444-4444-4444-4444-444444444444', 'BlitzByte', 'blitzbyte@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1712, 'https://api.dicebear.com/7.x/identicon/svg?seed=BlitzByte', 'US', null, 27, 22, 3, '2025-09-09T07:30:00Z', '2026-03-27T10:12:00Z'),
    ('55555555-5555-5555-5555-555555555555', 'ForkMaster', 'forkmaster@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1654, 'https://api.dicebear.com/7.x/identicon/svg?seed=ForkMaster', 'GB', null, 21, 20, 6, '2025-10-19T16:20:00Z', '2026-03-27T10:10:00Z'),
    ('66666666-6666-6666-6666-666666666666', 'SicilianStorm', 'sicilianstorm@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1545, 'https://api.dicebear.com/7.x/identicon/svg?seed=SicilianStorm', 'IT', null, 14, 18, 4, '2025-12-03T18:10:00Z', '2026-03-25T11:40:00Z'),
    ('77777777-7777-7777-7777-777777777777', 'PuzzlePriya', 'puzzlepriya@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1498, 'https://api.dicebear.com/7.x/identicon/svg?seed=PuzzlePriya', 'IN', null, 11, 14, 9, '2026-01-11T05:45:00Z', '2026-03-24T15:20:00Z'),
    ('88888888-8888-8888-8888-888888888888', 'KnightCrawler', 'knightcrawler@chessmaster.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1432, 'https://api.dicebear.com/7.x/identicon/svg?seed=KnightCrawler', 'CA', null, 9, 17, 5, '2026-02-04T14:25:00Z', '2026-03-23T12:50:00Z')
on conflict (id) do update set
    username = excluded.username,
    email = excluded.email,
    password_hash = excluded.password_hash,
    rating = excluded.rating,
    avatar_url = excluded.avatar_url,
    country = excluded.country,
    title = excluded.title,
    wins_count = excluded.wins_count,
    losses_count = excluded.losses_count,
    draws_count = excluded.draws_count,
    joined_at = excluded.joined_at,
    updated_at = excluded.updated_at;

insert into public.user_settings (
    id, user_id, move_sounds, notification_sounds, game_alerts, chat_messages,
    board_theme, default_time_control, created_at, updated_at
) values
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', true, true, true, true, 'classic', '10+0', '2025-11-10T09:01:00Z', '2026-03-27T09:30:00Z'),
    ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '22222222-2222-2222-2222-222222222222', false, false, true, false, 'midnight', '3+2', '2025-06-14T10:01:00Z', '2026-03-27T08:45:00Z'),
    ('aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '33333333-3333-3333-3333-333333333333', true, true, true, true, 'wood', '15+10', '2025-08-01T12:01:00Z', '2026-03-26T21:15:00Z'),
    ('aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '44444444-4444-4444-4444-444444444444', true, false, true, true, 'neon', '3+0', '2025-09-09T07:31:00Z', '2026-03-27T10:12:00Z'),
    ('aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '55555555-5555-5555-5555-555555555555', true, true, false, true, 'classic', '5+3', '2025-10-19T16:21:00Z', '2026-03-27T10:10:00Z'),
    ('aaaaaaa6-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '66666666-6666-6666-6666-666666666666', false, true, true, true, 'tournament', '10+5', '2025-12-03T18:11:00Z', '2026-03-25T11:40:00Z'),
    ('aaaaaaa7-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '77777777-7777-7777-7777-777777777777', true, true, true, false, 'classic', '10+0', '2026-01-11T05:46:00Z', '2026-03-24T15:20:00Z'),
    ('aaaaaaa8-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '88888888-8888-8888-8888-888888888888', true, true, false, true, 'wood', '1+0', '2026-02-04T14:26:00Z', '2026-03-23T12:50:00Z')
on conflict (user_id) do update set
    move_sounds = excluded.move_sounds,
    notification_sounds = excluded.notification_sounds,
    game_alerts = excluded.game_alerts,
    chat_messages = excluded.chat_messages,
    board_theme = excluded.board_theme,
    default_time_control = excluded.default_time_control,
    updated_at = excluded.updated_at;

insert into public.achievements (id, name, description) values
    ('90000000-0000-0000-0000-000000000001', 'First Win', 'Win your first rated or casual game.'),
    ('90000000-0000-0000-0000-000000000002', 'Speed Demon', 'Win a blitz game with under 10 seconds left.'),
    ('90000000-0000-0000-0000-000000000003', 'Analyst', 'Review three completed games in analysis mode.'),
    ('90000000-0000-0000-0000-000000000004', 'Chatty Player', 'Send 25 game chat messages.'),
    ('90000000-0000-0000-0000-000000000005', 'Iron Defender', 'Hold a drawn endgame after being down material.')
on conflict (id) do update set
    name = excluded.name,
    description = excluded.description;

insert into public.user_achievements (id, user_id, achievement_id, earned, earned_at) values
    ('91000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '90000000-0000-0000-0000-000000000001', true, '2025-11-12T14:00:00Z'),
    ('91000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '90000000-0000-0000-0000-000000000003', true, '2026-02-11T17:20:00Z'),
    ('91000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '90000000-0000-0000-0000-000000000004', true, '2026-03-01T18:40:00Z'),
    ('91000000-0000-0000-0000-000000000004', '33333333-3333-3333-3333-333333333333', '90000000-0000-0000-0000-000000000005', true, '2026-01-29T21:00:00Z'),
    ('91000000-0000-0000-0000-000000000005', '44444444-4444-4444-4444-444444444444', '90000000-0000-0000-0000-000000000002', true, '2026-03-09T12:05:00Z')
on conflict (user_id, achievement_id) do update set
    earned = excluded.earned,
    earned_at = excluded.earned_at;

insert into public.games (
    id, white_player_id, black_player_id, status, result, result_reason, fen, pgn, history_json,
    time_control, white_time_remaining, black_time_remaining, turn_color, last_move_from, last_move_to,
    last_move_san, rated, draw_offered_by, is_bot_game, bot_level, created_at, updated_at, ended_at
) values
    (
        'a1000000-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        '33333333-3333-3333-3333-333333333333',
        'FINISHED',
        'WHITE_WIN',
        'CHECKMATE',
        'move:7:h5:f7',
        'e2-e4 e7-e5 g1-f3 b8-c6 f1-c4 g8-f6 h5-f7',
        '["e2-e4","e7-e5","g1-f3","b8-c6","f1-c4","g8-f6","h5-f7"]',
        '10+0',
        418,
        372,
        'black',
        'h5',
        'f7',
        'Qxf7#',
        true,
        null,
        false,
        null,
        '2026-03-20T18:00:00Z',
        '2026-03-20T18:13:00Z',
        '2026-03-20T18:13:00Z'
    ),
    (
        'a1000000-0000-0000-0000-000000000002',
        '44444444-4444-4444-4444-444444444444',
        '55555555-5555-5555-5555-555555555555',
        'ACTIVE',
        null,
        null,
        'move:6:f1:c4',
        'e2-e4 c7-c5 g1-f3 d7-d6 f1-c4',
        '["e2-e4","c7-c5","g1-f3","d7-d6","f1-c4"]',
        '3+2',
        96,
        104,
        'black',
        'f1',
        'c4',
        'Bc4',
        true,
        null,
        false,
        null,
        '2026-03-27T10:00:00Z',
        '2026-03-27T10:04:00Z',
        null
    ),
    (
        'a1000000-0000-0000-0000-000000000003',
        '11111111-1111-1111-1111-111111111111',
        null,
        'ACTIVE',
        null,
        null,
        'move:4:f1:b5',
        'e2-e4 e7-e5 g1-f3 b8-c6 f1-b5',
        '["e2-e4","e7-e5","g1-f3","b8-c6","f1-b5"]',
        '5+0',
        255,
        268,
        'black',
        'f1',
        'b5',
        'Bb5',
        false,
        null,
        true,
        3,
        '2026-03-27T09:50:00Z',
        '2026-03-27T09:55:00Z',
        null
    ),
    (
        'a1000000-0000-0000-0000-000000000004',
        '66666666-6666-6666-6666-666666666666',
        '11111111-1111-1111-1111-111111111111',
        'FINISHED',
        'DRAW',
        'DRAW_AGREED',
        'move:8:c2:c4',
        'e2-e4 c7-c5 g1-f3 d7-d6 d2-d4 c5-d4 f3-d4 g8-f6 c2-c4',
        '["e2-e4","c7-c5","g1-f3","d7-d6","d2-d4","c5-d4","f3-d4","g8-f6","c2-c4"]',
        '15+10',
        732,
        745,
        'black',
        'c2',
        'c4',
        'c4',
        false,
        null,
        false,
        null,
        '2026-03-18T17:30:00Z',
        '2026-03-18T17:49:00Z',
        '2026-03-18T17:49:00Z'
    )
on conflict (id) do update set
    white_player_id = excluded.white_player_id,
    black_player_id = excluded.black_player_id,
    status = excluded.status,
    result = excluded.result,
    result_reason = excluded.result_reason,
    fen = excluded.fen,
    pgn = excluded.pgn,
    history_json = excluded.history_json,
    time_control = excluded.time_control,
    white_time_remaining = excluded.white_time_remaining,
    black_time_remaining = excluded.black_time_remaining,
    turn_color = excluded.turn_color,
    last_move_from = excluded.last_move_from,
    last_move_to = excluded.last_move_to,
    last_move_san = excluded.last_move_san,
    rated = excluded.rated,
    draw_offered_by = excluded.draw_offered_by,
    is_bot_game = excluded.is_bot_game,
    bot_level = excluded.bot_level,
    updated_at = excluded.updated_at,
    ended_at = excluded.ended_at;

insert into public.game_moves (
    id, game_id, player_id, move_number, move_color, from_square, to_square, promotion, san, fen_after, created_at
) values
    ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 1, 'white', 'e2', 'e4', null, 'e2-e4', 'move:1:e2:e4', '2026-03-20T18:00:10Z'),
    ('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 2, 'black', 'e7', 'e5', null, 'e7-e5', 'move:2:e7:e5', '2026-03-20T18:01:00Z'),
    ('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 3, 'white', 'g1', 'f3', null, 'g1-f3', 'move:3:g1:f3', '2026-03-20T18:02:00Z'),
    ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 4, 'black', 'b8', 'c6', null, 'b8-c6', 'move:4:b8:c6', '2026-03-20T18:03:20Z'),
    ('b1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 5, 'white', 'f1', 'c4', null, 'f1-c4', 'move:5:f1:c4', '2026-03-20T18:05:00Z'),
    ('b1000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 6, 'black', 'g8', 'f6', null, 'g8-f6', 'move:6:g8:f6', '2026-03-20T18:07:00Z'),
    ('b1000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 7, 'white', 'h5', 'f7', null, 'Qxf7#', 'move:7:h5:f7', '2026-03-20T18:13:00Z'),
    ('b1000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 1, 'white', 'e2', 'e4', null, 'e2-e4', 'move:1:e2:e4', '2026-03-27T10:00:12Z'),
    ('b1000000-0000-0000-0000-000000000009', 'a1000000-0000-0000-0000-000000000002', '55555555-5555-5555-5555-555555555555', 2, 'black', 'c7', 'c5', null, 'c7-c5', 'move:2:c7:c5', '2026-03-27T10:00:35Z'),
    ('b1000000-0000-0000-0000-000000000010', 'a1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 3, 'white', 'g1', 'f3', null, 'g1-f3', 'move:3:g1:f3', '2026-03-27T10:01:10Z'),
    ('b1000000-0000-0000-0000-000000000011', 'a1000000-0000-0000-0000-000000000002', '55555555-5555-5555-5555-555555555555', 4, 'black', 'd7', 'd6', null, 'd7-d6', 'move:4:d7:d6', '2026-03-27T10:02:30Z'),
    ('b1000000-0000-0000-0000-000000000012', 'a1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 5, 'white', 'f1', 'c4', null, 'Bc4', 'move:5:f1:c4', '2026-03-27T10:04:00Z'),
    ('b1000000-0000-0000-0000-000000000013', 'a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 1, 'white', 'e2', 'e4', null, 'e2-e4', 'move:1:e2:e4', '2026-03-27T09:50:10Z'),
    ('b1000000-0000-0000-0000-000000000014', 'a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 2, 'black', 'e7', 'e5', null, 'e7-e5', 'move:2:e7:e5', '2026-03-27T09:51:00Z'),
    ('b1000000-0000-0000-0000-000000000015', 'a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 3, 'white', 'g1', 'f3', null, 'g1-f3', 'move:3:g1:f3', '2026-03-27T09:52:00Z'),
    ('b1000000-0000-0000-0000-000000000016', 'a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 4, 'black', 'b8', 'c6', null, 'b8-c6', 'move:4:b8:c6', '2026-03-27T09:53:10Z'),
    ('b1000000-0000-0000-0000-000000000017', 'a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 5, 'white', 'f1', 'b5', null, 'Bb5', 'move:5:f1:b5', '2026-03-27T09:55:00Z')
on conflict (id) do update set
    game_id = excluded.game_id,
    player_id = excluded.player_id,
    move_number = excluded.move_number,
    move_color = excluded.move_color,
    from_square = excluded.from_square,
    to_square = excluded.to_square,
    promotion = excluded.promotion,
    san = excluded.san,
    fen_after = excluded.fen_after,
    created_at = excluded.created_at;

insert into public.game_chat_messages (id, game_id, sender_id, message, created_at) values
    ('c1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'GLHF!', '2026-03-27T10:00:05Z'),
    ('c1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002', '55555555-5555-5555-5555-555555555555', 'Have fun.', '2026-03-27T10:00:15Z'),
    ('c1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'Najdorf incoming?', '2026-03-27T10:01:22Z'),
    ('c1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002', '55555555-5555-5555-5555-555555555555', 'Only if you let me.', '2026-03-27T10:01:40Z')
on conflict (id) do update set
    game_id = excluded.game_id,
    sender_id = excluded.sender_id,
    message = excluded.message,
    created_at = excluded.created_at;

insert into public.puzzle_tags (id, name, slug) values
    ('92000000-0000-0000-0000-000000000001', 'Mate', 'mate'),
    ('92000000-0000-0000-0000-000000000002', 'Back Rank', 'back-rank'),
    ('92000000-0000-0000-0000-000000000003', 'Fork', 'fork'),
    ('92000000-0000-0000-0000-000000000004', 'Endgame', 'endgame'),
    ('92000000-0000-0000-0000-000000000005', 'Promotion', 'promotion'),
    ('92000000-0000-0000-0000-000000000006', 'Queen', 'queen'),
    ('92000000-0000-0000-0000-000000000007', 'Rook', 'rook'),
    ('92000000-0000-0000-0000-000000000008', 'Knight', 'knight')
on conflict (id) do update set
    name = excluded.name,
    slug = excluded.slug;

insert into public.puzzles (
    id, slug, title, description, fen, difficulty, primary_theme, max_wrong_attempts, active, created_at, updated_at
) values
    ('93000000-0000-0000-0000-000000000001', 'queen-lift-d8', 'Queen Lift to d8', 'White finishes with a direct queen lift.', '6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1', 'EASY', 'mate', 2, true, '2026-04-01T00:00:00Z', '2026-04-01T00:00:00Z'),
    ('93000000-0000-0000-0000-000000000002', 'rook-file-e8', 'Open File Rook Finish', 'Use the open e-file to end the game.', '6k1/6pp/8/8/8/8/8/4R1K1 w - - 0 1', 'EASY', 'back-rank', 2, true, '2026-04-01T00:01:00Z', '2026-04-01T00:01:00Z'),
    ('93000000-0000-0000-0000-000000000003', 'queen-from-home', 'Queen From Home', 'The back rank is unguarded.', '6k1/5ppp/8/8/8/8/8/3Q2K1 w - - 0 1', 'EASY', 'mate', 2, true, '2026-04-01T00:02:00Z', '2026-04-01T00:02:00Z'),
    ('93000000-0000-0000-0000-000000000004', 'rook-sweep-a8', 'Rook Sweep', 'One clean rook move seals the board.', '7k/6pp/8/8/8/8/8/R5K1 w - - 0 1', 'EASY', 'back-rank', 2, true, '2026-04-01T00:03:00Z', '2026-04-01T00:03:00Z'),
    ('93000000-0000-0000-0000-000000000005', 'queen-diagonal-e8', 'Diagonal Queen Finish', 'Spot the diagonal route to mate.', '6k1/5ppp/8/7Q/8/8/8/6K1 w - - 0 1', 'EASY', 'mate', 2, true, '2026-04-01T00:04:00Z', '2026-04-01T00:04:00Z'),
    ('93000000-0000-0000-0000-000000000006', 'knight-fork-d7', 'Knight Fork', 'Jump into a fork on the heavy pieces.', '6k1/3q1ppp/8/4N3/8/8/8/6K1 w - - 0 1', 'EASY', 'fork', 2, true, '2026-04-01T00:05:00Z', '2026-04-01T00:05:00Z'),
    ('93000000-0000-0000-0000-000000000007', 'queen-file-e8', 'Queen File Breakthrough', 'The e-file is the key.', '6k1/6pp/8/8/8/8/4Q3/6K1 w - - 0 1', 'MEDIUM', 'queen', 2, true, '2026-04-01T00:06:00Z', '2026-04-01T00:06:00Z'),
    ('93000000-0000-0000-0000-000000000008', 'queen-file-d8-2', 'Second Rank Queen Lift', 'Bring the queen all the way up the file.', '6k1/6pp/8/8/8/8/3Q4/6K1 w - - 0 1', 'MEDIUM', 'queen', 2, true, '2026-04-01T00:07:00Z', '2026-04-01T00:07:00Z'),
    ('93000000-0000-0000-0000-000000000009', 'queen-file-c8', 'Quiet Queen Finish', 'A calm queen move wins immediately.', '6k1/6pp/8/8/8/8/2Q5/6K1 w - - 0 1', 'MEDIUM', 'queen', 2, true, '2026-04-01T00:08:00Z', '2026-04-01T00:08:00Z'),
    ('93000000-0000-0000-0000-000000000010', 'queen-file-b8', 'Long Queen Lift', 'Travel the full file and finish the king.', '6k1/6pp/8/8/8/8/1Q6/6K1 w - - 0 1', 'MEDIUM', 'queen', 2, true, '2026-04-01T00:09:00Z', '2026-04-01T00:09:00Z'),
    ('93000000-0000-0000-0000-000000000011', 'rook-file-a8', 'Open a-File Rook Mate', 'The a-file is completely open.', '6k1/6pp/8/8/8/8/R7/6K1 w - - 0 1', 'MEDIUM', 'rook', 2, true, '2026-04-01T00:10:00Z', '2026-04-01T00:10:00Z'),
    ('93000000-0000-0000-0000-000000000012', 'rook-file-d8', 'Central Rook Finish', 'Centralize and crash through.', '6k1/6pp/8/8/8/8/8/3R2K1 w - - 0 1', 'MEDIUM', 'rook', 2, true, '2026-04-01T00:11:00Z', '2026-04-01T00:11:00Z'),
    ('93000000-0000-0000-0000-000000000013', 'rook-file-b8', 'Second File Rook Mate', 'Another open file tactic with the rook.', '6k1/6pp/8/8/8/8/8/1R4K1 w - - 0 1', 'MEDIUM', 'back-rank', 2, true, '2026-04-01T00:12:00Z', '2026-04-01T00:12:00Z'),
    ('93000000-0000-0000-0000-000000000014', 'rook-file-c8', 'Third File Rook Mate', 'Use the rook from c1.', '6k1/6pp/8/8/8/8/8/2R3K1 w - - 0 1', 'HARD', 'back-rank', 2, true, '2026-04-01T00:13:00Z', '2026-04-01T00:13:00Z'),
    ('93000000-0000-0000-0000-000000000015', 'rook-file-f8', 'Rook Swing to f8', 'The far rook still has time to finish.', '6k1/6pp/8/8/8/8/8/5RK1 w - - 0 1', 'HARD', 'rook', 2, true, '2026-04-01T00:14:00Z', '2026-04-01T00:14:00Z'),
    ('93000000-0000-0000-0000-000000000016', 'promotion-h8q', 'Promotion Race', 'Promote with check at the right moment.', '5k2/7P/6K1/8/8/8/8/8 w - - 0 1', 'HARD', 'promotion', 2, true, '2026-04-01T00:15:00Z', '2026-04-01T00:15:00Z'),
    ('93000000-0000-0000-0000-000000000017', 'promotion-a8q', 'Outside Passed Pawn', 'Promote the outside passer.', '1k6/P7/1K6/8/8/8/8/8 w - - 0 1', 'HARD', 'endgame', 2, true, '2026-04-01T00:16:00Z', '2026-04-01T00:16:00Z'),
    ('93000000-0000-0000-0000-000000000018', 'queen-file-f8', 'Long Queen File', 'The f-file is clear for a direct finish.', '6k1/6pp/8/8/8/8/5Q2/6K1 w - - 0 1', 'HARD', 'queen', 2, true, '2026-04-01T00:17:00Z', '2026-04-01T00:17:00Z'),
    ('93000000-0000-0000-0000-000000000019', 'queen-file-g8', 'Direct Queen Climb', 'A direct climb to the back rank wins.', '6k1/6pp/8/8/8/8/6Q1/6K1 w - - 0 1', 'MEDIUM', 'queen', 2, true, '2026-04-01T00:18:00Z', '2026-04-01T00:18:00Z'),
    ('93000000-0000-0000-0000-000000000020', 'queen-file-h8', 'Edge File Queen Finish', 'Use the edge file accurately.', '6k1/6pp/8/8/8/8/7Q/6K1 w - - 0 1', 'EASY', 'queen', 2, true, '2026-04-01T00:19:00Z', '2026-04-01T00:19:00Z')
on conflict (id) do update set
    slug = excluded.slug,
    title = excluded.title,
    description = excluded.description,
    fen = excluded.fen,
    difficulty = excluded.difficulty,
    primary_theme = excluded.primary_theme,
    max_wrong_attempts = excluded.max_wrong_attempts,
    active = excluded.active,
    updated_at = excluded.updated_at;

insert into public.puzzle_tag_links (puzzle_id, tag_id) values
    ('93000000-0000-0000-0000-000000000001', '92000000-0000-0000-0000-000000000001'),
    ('93000000-0000-0000-0000-000000000001', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000002', '92000000-0000-0000-0000-000000000002'),
    ('93000000-0000-0000-0000-000000000002', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000003', '92000000-0000-0000-0000-000000000001'),
    ('93000000-0000-0000-0000-000000000003', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000004', '92000000-0000-0000-0000-000000000002'),
    ('93000000-0000-0000-0000-000000000004', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000005', '92000000-0000-0000-0000-000000000001'),
    ('93000000-0000-0000-0000-000000000005', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000006', '92000000-0000-0000-0000-000000000003'),
    ('93000000-0000-0000-0000-000000000006', '92000000-0000-0000-0000-000000000008'),
    ('93000000-0000-0000-0000-000000000007', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000008', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000009', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000010', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000011', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000012', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000013', '92000000-0000-0000-0000-000000000002'),
    ('93000000-0000-0000-0000-000000000013', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000014', '92000000-0000-0000-0000-000000000002'),
    ('93000000-0000-0000-0000-000000000014', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000015', '92000000-0000-0000-0000-000000000007'),
    ('93000000-0000-0000-0000-000000000016', '92000000-0000-0000-0000-000000000004'),
    ('93000000-0000-0000-0000-000000000016', '92000000-0000-0000-0000-000000000005'),
    ('93000000-0000-0000-0000-000000000017', '92000000-0000-0000-0000-000000000004'),
    ('93000000-0000-0000-0000-000000000017', '92000000-0000-0000-0000-000000000005'),
    ('93000000-0000-0000-0000-000000000018', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000019', '92000000-0000-0000-0000-000000000006'),
    ('93000000-0000-0000-0000-000000000020', '92000000-0000-0000-0000-000000000006')
on conflict do nothing;

insert into public.puzzle_solution_steps (
    id, puzzle_id, step_number, move_uci, move_san, side_to_move, is_opponent_move
) values
    ('93100000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', 1, 'd5d8', 'Qd8#', 'white', false),
    ('93100000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000002', 1, 'e1e8', 'Re8#', 'white', false),
    ('93100000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000003', 1, 'd1d8', 'Qd8#', 'white', false),
    ('93100000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000004', 1, 'a1a8', 'Ra8#', 'white', false),
    ('93100000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000005', 1, 'h5e8', 'Qe8#', 'white', false),
    ('93100000-0000-0000-0000-000000000006', '93000000-0000-0000-0000-000000000006', 1, 'e5d7', 'Nd7', 'white', false),
    ('93100000-0000-0000-0000-000000000007', '93000000-0000-0000-0000-000000000007', 1, 'e2e8', 'Qe8#', 'white', false),
    ('93100000-0000-0000-0000-000000000008', '93000000-0000-0000-0000-000000000008', 1, 'd2d8', 'Qd8#', 'white', false),
    ('93100000-0000-0000-0000-000000000009', '93000000-0000-0000-0000-000000000009', 1, 'c2c8', 'Qc8#', 'white', false),
    ('93100000-0000-0000-0000-000000000010', '93000000-0000-0000-0000-000000000010', 1, 'b2b8', 'Qb8#', 'white', false),
    ('93100000-0000-0000-0000-000000000011', '93000000-0000-0000-0000-000000000011', 1, 'a2a8', 'Ra8#', 'white', false),
    ('93100000-0000-0000-0000-000000000012', '93000000-0000-0000-0000-000000000012', 1, 'd1d8', 'Rd8#', 'white', false),
    ('93100000-0000-0000-0000-000000000013', '93000000-0000-0000-0000-000000000013', 1, 'b1b8', 'Rb8#', 'white', false),
    ('93100000-0000-0000-0000-000000000014', '93000000-0000-0000-0000-000000000014', 1, 'c1c8', 'Rc8#', 'white', false),
    ('93100000-0000-0000-0000-000000000015', '93000000-0000-0000-0000-000000000015', 1, 'f1f8', 'Rf8#', 'white', false),
    ('93100000-0000-0000-0000-000000000016', '93000000-0000-0000-0000-000000000016', 1, 'h7h8q', 'h8=Q+', 'white', false),
    ('93100000-0000-0000-0000-000000000017', '93000000-0000-0000-0000-000000000017', 1, 'a7a8q', 'a8=Q+', 'white', false),
    ('93100000-0000-0000-0000-000000000018', '93000000-0000-0000-0000-000000000018', 1, 'f2f8', 'Qf8#', 'white', false),
    ('93100000-0000-0000-0000-000000000019', '93000000-0000-0000-0000-000000000019', 1, 'g2g8', 'Qg8#', 'white', false),
    ('93100000-0000-0000-0000-000000000020', '93000000-0000-0000-0000-000000000020', 1, 'h2h8', 'Qh8#', 'white', false)
on conflict (id) do update set
    puzzle_id = excluded.puzzle_id,
    step_number = excluded.step_number,
    move_uci = excluded.move_uci,
    move_san = excluded.move_san,
    side_to_move = excluded.side_to_move,
    is_opponent_move = excluded.is_opponent_move;

commit;
