# ChessMaster Pro Frontend To Current Backend Handoff

This document is for a frontend engineer or frontend API agent.

It describes the backend exactly as it exists now in this repository, so the frontend can be updated to use the real Spring Boot API instead of local state and localStorage.

Use this document as the source of truth for frontend integration.

## Goal

Replace the frontend's mock or local-only behavior with live calls to the Spring Boot backend running at:

- Base URL: `http://localhost:8081`
- WebSocket endpoint: `ws://localhost:8081/ws-chess`

The backend uses:

- REST JSON APIs
- JWT bearer authentication
- STOMP over WebSocket for game updates and chat
- PostgreSQL/Supabase persistence

## Important Reality Check

The backend now supports real:

1. register
2. login
3. current user
4. settings persistence
5. profile data
6. leaderboard data
7. game persistence
8. move submission
9. resign/draw flows
10. chat persistence
11. matchmaking endpoints
12. bot game creation
13. STOMP messaging

But some modules are still placeholder-quality and the frontend should treat them accordingly:

1. game legality is not full chess-engine validation yet
2. FEN values are currently placeholder strings during live play
3. PGN is stored as a simple joined move list, not a full engine-generated PGN
4. bot gameplay is engine-backed, but requires Stockfish executable on backend host
5. analysis responses are Stockfish-backed for valid PGN/FEN input

Frontend integration should move to backend bot APIs and treat backend as authoritative for bot moves.

## Authentication Model

The backend auth flow is real and should replace frontend localStorage auth.

### Token Handling

After login or registration, the backend returns:

```json
{
  "token": "jwt-token",
  "user": {
    "id": "uuid",
    "username": "TacticalTiger",
    "email": "tacticaltiger@chessmaster.dev",
    "rating": 1825,
    "avatarUrl": "https://...",
    "country": "IN",
    "title": "CM",
    "joinedAt": "2025-11-10T09:00:00Z"
  },
  "settings": {
    "moveSounds": true,
    "notificationSounds": true,
    "gameAlerts": true,
    "chatMessages": true,
    "boardTheme": "classic",
    "defaultTimeControl": "10+0"
  }
}
```

Frontend requirements:

1. store `token`
2. send `Authorization: Bearer <token>` on protected REST requests
3. send the same bearer token in the STOMP `CONNECT` headers
4. stop using the old localStorage user/session format

### Auth Endpoints

#### `POST /api/auth/register`

Request:

```json
{
  "username": "NewPlayer",
  "email": "newplayer@example.com",
  "password": "Password123!"
}
```

Rules:

1. `username`: 3 to 50 chars
2. `email`: valid email
3. `password`: 8 to 128 chars

Success response:

```json
{
  "token": "jwt-token",
  "user": {
    "id": "uuid",
    "username": "NewPlayer",
    "email": "newplayer@example.com",
    "rating": 1500,
    "avatarUrl": null,
    "country": null,
    "title": null,
    "joinedAt": "2026-03-27T10:00:00Z"
  },
  "settings": {
    "moveSounds": true,
    "notificationSounds": true,
    "gameAlerts": true,
    "chatMessages": true,
    "boardTheme": "classic",
    "defaultTimeControl": "10+0"
  }
}
```

Common errors:

1. `400 username_taken`
2. `400 email_taken`
3. `400 validation_failed`

#### `POST /api/auth/login`

Important: login uses `email`, not username.

Request:

```json
{
  "email": "newplayer@example.com",
  "password": "Password123!"
}
```

Success response:

Same shape as register.

Common errors:

1. `401 invalid_credentials`
2. `400 validation_failed`

#### `GET /api/auth/me`

Requires bearer token.

Returns the same `AuthResponse` shape:

```json
{
  "token": "jwt-token",
  "user": { "...": "..." },
  "settings": { "...": "..." }
}
```

Frontend note:

The backend currently returns a fresh token again on `me`. The frontend may keep using the existing token or replace it.

#### `POST /api/auth/logout`

Requires no request body.

Response:

- `204 No Content`

Frontend note:

Logout is currently client-side token removal. There is no token blacklist/session invalidation yet.

## Settings APIs

These should replace the frontend settings localStorage flow.

### `GET /api/settings`

Requires bearer token.

Response:

```json
{
  "moveSounds": true,
  "notificationSounds": true,
  "gameAlerts": true,
  "chatMessages": true,
  "boardTheme": "classic",
  "defaultTimeControl": "10+0"
}
```

### `PATCH /api/settings`

Requires bearer token.

Partial updates are supported. Any omitted field is left unchanged.

Request:

```json
{
  "moveSounds": false,
  "boardTheme": "wood",
  "defaultTimeControl": "5+0"
}
```

Response:

Same shape as `GET /api/settings`.

## Game APIs

These should replace frontend local game state for authenticated play.

### Current Backend Limitations To Respect

Frontend should not assume:

1. full server chess-engine legality
2. true FEN generation
3. true SAN generation
4. clock ticking pushed every second

Frontend can still use these APIs as the source of persisted state and multiplayer flow.

### `POST /api/games`

Requires bearer token.

Request:

```json
{
  "timeControl": "10+0",
  "rated": true,
  "colorPreference": "random"
}
```

Allowed `colorPreference` values expected by backend behavior:

1. `white`
2. `black`
3. `random`

Response shape:

```json
{
  "gameId": "uuid",
  "whitePlayerId": "uuid",
  "blackPlayerId": null,
  "fen": "startpos",
  "pgn": "",
  "history": [],
  "timeControl": "10+0",
  "whiteTimeRemaining": 600,
  "blackTimeRemaining": 600,
  "status": "ACTIVE",
  "result": null,
  "resultReason": null,
  "lastMove": null,
  "rated": true,
  "isBotGame": false,
  "botLevel": null,
  "turnColor": "white",
  "drawOfferedBy": null,
  "createdAt": "2026-03-27T10:00:00Z",
  "updatedAt": "2026-03-27T10:00:00Z"
}
```

### `GET /api/games/{gameId}`

Requires bearer token.

Important:

Only game participants can fetch a game. If the frontend expects spectators, that is not implemented yet.

### `POST /api/games/{gameId}/move`

Requires bearer token.

Request:

```json
{
  "from": "e2",
  "to": "e4",
  "promotion": "q"
}
```

`promotion` is optional.

Response:

Same `GameResponse` shape as above, but updated.

Common errors:

1. `400 invalid_square`
2. `403 not_game_participant`
3. `404 game_not_found`
4. `409 game_not_active`
5. `409 not_your_turn`

### `POST /api/games/{gameId}/resign`

Requires bearer token.

Response:

Updated `GameResponse` with:

1. `status = "FINISHED"`
2. `result = "WHITE_WIN"` or `"BLACK_WIN"`
3. `resultReason = "RESIGNATION"`

### `POST /api/games/{gameId}/draw-offer`

Requires bearer token.

Response:

Updated `GameResponse` with `drawOfferedBy` set to the offering user's id.

### `POST /api/games/{gameId}/draw-respond`

Requires bearer token.

Request:

```json
{
  "accepted": true
}
```

Behavior:

1. if accepted, game becomes finished with `result = "DRAW"` and `resultReason = "DRAW_AGREED"`
2. if rejected, draw offer is simply cleared

### `GET /api/games/{gameId}/pgn`

Requires bearer token.

Response:

```json
{
  "pgn": "e2-e4 e7-e5 ..."
}
```

### `GET /api/games/{gameId}/fen`

Requires bearer token.

Response:

```json
{
  "fen": "move:5:f1:c4"
}
```

Important:

Current FEN values are placeholders during gameplay, not true chess FEN strings.

## Bot Game API

### `POST /api/bot-games`

Requires bearer token.

Request:

```json
{
  "level": 3,
  "color": "white"
}
```

Response:

Standard `GameResponse` with:

1. `isBotGame = true`
2. `botLevel = <level>`

Important:

This creates persisted bot games and backend now auto-plays bot turns using Stockfish. Backend must have `stockfish` available in PATH, or set `BOT_STOCKFISH_PATH` in backend environment.

## Chat APIs

These should replace local-only chat.

### `GET /api/games/{gameId}/chat`

Requires bearer token.

Response:

```json
[
  {
    "id": "uuid",
    "gameId": "uuid",
    "senderId": "uuid",
    "senderUsername": "BlitzByte",
    "message": "GLHF!",
    "createdAt": "2026-03-27T10:00:05Z"
  }
]
```

### `POST /api/games/{gameId}/chat`

Requires bearer token.

Request:

```json
{
  "message": "Nice move"
}
```

Response:

Same `GameChatMessageResponse` shape as above.

## Profile APIs

### `GET /api/profile/me`

Requires bearer token.

Response:

```json
{
  "user": {
    "id": "uuid",
    "username": "TacticalTiger",
    "email": "tacticaltiger@chessmaster.dev",
    "rating": 1825,
    "avatarUrl": "https://...",
    "country": "IN",
    "title": "CM",
    "joinedAt": "2025-11-10T09:00:00Z"
  },
  "ratings": {
    "rapid": 1825,
    "blitz": 1790,
    "bullet": 1765
  },
  "aggregateStats": {
    "gamesPlayed": 10,
    "wins": 32,
    "losses": 11,
    "draws": 4
  },
  "recentGames": [
    {
      "gameId": "uuid"
    }
  ],
  "achievements": [
    {
      "id": "uuid",
      "name": "First Win",
      "description": "Win your first rated or casual game.",
      "earned": true,
      "earnedAt": "2025-11-12T14:00:00Z"
    }
  ]
}
```

### `GET /api/profile/{userId}`

Public.

Returns the same shape as `/api/profile/me`.

### `GET /api/profile/{userId}/games`

Public.

Returns:

```json
[
  {
    "gameId": "uuid",
    "...": "GameResponse"
  }
]
```

### `GET /api/profile/{userId}/achievements`

Public.

Returns:

```json
[
  {
    "id": "uuid",
    "name": "First Win",
    "description": "Win your first rated or casual game.",
    "earned": true,
    "earnedAt": "2025-11-12T14:00:00Z"
  }
]
```

## Leaderboard API

### `GET /api/leaderboard`

Public.

Optional query param:

- `query`

Example:

`GET /api/leaderboard?query=mag`

Response:

```json
[
  {
    "rank": 1,
    "username": "MagnusMock",
    "title": "GM",
    "country": "NO",
    "rating": 2410,
    "change": 0,
    "gamesPlayed": 147,
    "winRate": 81.6
  }
]
```

Important:

The backend currently ignores frontend time-control tabs. There is no real `timeControl` filter on leaderboard yet.

Frontend guidance:

1. wire the page to real data now
2. if the UI has rapid/blitz/bullet tabs, either keep them client-side for now or map all tabs to the same endpoint until backend filtering is added

### `GET /api/leaderboard/fide`

Public.

This is a separate leaderboard backed by imported official FIDE list data.

Optional query params:

- `query`
- `timeControl` = `standard` | `rapid` | `blitz`
- `country` = federation code like `IND`
- `gender` = `open` | `male` | `female`
- `division` = `open` | `junior` | `senior`
- `page`
- `size`
- `activeOnly`

Example:

`GET /api/leaderboard/fide?timeControl=rapid&country=IND&gender=female&division=junior`

Response:

```json
{
  "timeControl": "rapid",
  "gender": "female",
  "division": "junior",
  "country": "IND",
  "query": null,
  "page": 0,
  "size": 50,
  "totalEntries": 1,
  "lastSyncedAt": "2026-04-10T10:00:00Z",
  "entries": [
    {
      "rank": 1,
      "fideId": 35009192,
      "name": "Divya Deshmukh",
      "title": "IM",
      "country": "IND",
      "gender": "F",
      "birthYear": 2005,
      "timeControl": "rapid",
      "rating": 2395,
      "gamesPlayed": 11,
      "inactive": false
    }
  ]
}
```

## Analysis APIs

### `POST /api/analysis/pgn`

Public.

Request:

```json
{
  "pgn": "1. e4 e5 2. Nf3 Nc6"
}
```

### `POST /api/analysis/fen`

Public.

Request:

```json
{
  "fen": "some-fen-or-placeholder"
}
```

Response shape for both:

```json
{
  "analysisId": "uuid",
  "sourceType": "PGN",
  "bestMove": "e2e4",
  "evaluation": 0.2,
  "evaluationSeries": [0.2, 0.1, 0.4],
  "moveClassifications": ["book", "best", "good"],
  "valid": true
}
```

Important:

These responses are now engine-backed when the backend has Stockfish available. `bestMove` is returned in UCI format, `evaluation` is normalized to White's perspective, `evaluationSeries` represents the analyzed score after each played move for PGN input, and `moveClassifications` aligns one-to-one with the submitted moves.

## Matchmaking APIs

### `POST /api/matchmaking/join`

Requires bearer token.

Request:

```json
{
  "timeControl": "10+0",
  "rated": true
}
```

Response:

```json
{
  "searching": true,
  "timeControl": "10+0",
  "rated": true,
  "matchedGameId": null
}
```

If a match is found:

```json
{
  "searching": false,
  "timeControl": "10+0",
  "rated": true,
  "matchedGameId": "uuid"
}
```

### `POST /api/matchmaking/cancel`

Requires bearer token.

Response:

Same `MatchmakingStatusResponse` shape.

### `GET /api/matchmaking/status`

Requires bearer token.

Response:

Same `MatchmakingStatusResponse` shape.

Important:

Current matchmaking is in-memory. It is suitable for frontend integration/dev testing, not production scaling.

## WebSocket / STOMP Contract

The frontend should use STOMP.

### Connect

WebSocket endpoint:

- `ws://localhost:8081/ws-chess`

Send connect headers:

```json
{
  "Authorization": "Bearer <jwt>"
}
```

### Subscribe

Subscribe to:

- `/topic/game/{gameId}`

### Publish

#### Move

Publish to:

- `/app/game.move`

Payload:

```json
{
  "gameId": "uuid",
  "from": "e2",
  "to": "e4",
  "promotion": "q"
}
```

#### Chat

Publish to:

- `/app/game.chat`

Payload:

```json
{
  "gameId": "uuid",
  "message": "Nice move"
}
```

### Incoming Topic Messages

`/topic/game/{gameId}` currently receives mixed event shapes:

1. `GameResponse` when a move, resign, or draw flow updates the game
2. `GameChatMessageResponse` when a chat message is sent

This means the frontend subscriber must discriminate by payload shape at runtime.

Recommended temporary frontend strategy:

1. if payload contains `gameId`, `status`, `history`, and `timeControl`, treat it as a game-state update
2. if payload contains `senderUsername` and `message`, treat it as a chat message

Important:

There is not yet a typed WebSocket event envelope like `{ type, payload }`.

## Error Handling

The backend returns structured errors.

Generic error shape:

```json
{
  "timestamp": "2026-03-27T10:00:00Z",
  "status": 409,
  "error": "not_your_turn"
}
```

Validation error shape:

```json
{
  "timestamp": "2026-03-27T10:00:00Z",
  "status": 400,
  "error": "validation_failed",
  "details": {
    "email": "must be a well-formed email address"
  }
}
```

Frontend should:

1. surface `error` codes directly for predictable UX
2. map known auth/game error codes to friendly messages
3. handle `validation_failed.details` field-by-field in forms

## Immediate Frontend Migration Plan

The frontend/API agent should implement the migration in this order:

1. Replace localStorage auth with `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, and client-side token storage.
2. Replace local settings persistence with `/api/settings`.
3. Replace profile and leaderboard mock data with `/api/profile/*` and `/api/leaderboard`.
4. Replace game creation and move submission with `/api/games`, `/api/games/{id}/move`, `/api/games/{id}/resign`, and draw APIs.
5. Replace local chat with REST chat history plus STOMP chat updates.
6. Wire STOMP to `/ws-chess`, `/app/game.move`, `/app/game.chat`, and `/topic/game/{gameId}`.
7. Wire bot game creation to `/api/bot-games`.
8. Wire analysis page to `/api/analysis/pgn` and `/api/analysis/fen` as real backend analysis.
9. Wire matchmaking UI to `/api/matchmaking/*`.

## Final Prompt For Frontend API Agent

Use this prompt directly:

Update the ChessMaster Pro frontend so it uses the current Spring Boot backend running on `http://localhost:8081` instead of localStorage, mock data, or local-only chess state.

Important constraints:

1. Use the backend contract documented in this README exactly as it exists today.
2. Do not assume backend features that are not implemented yet.
3. Login uses `email` and `password`, not username.
4. Store the JWT token client-side and send `Authorization: Bearer <token>` on protected requests.
5. Use STOMP WebSocket at `ws://localhost:8081/ws-chess`.
6. Send the JWT in the STOMP `CONNECT` headers as `Authorization: Bearer <token>`.
7. Subscribe to `/topic/game/{gameId}`.
8. Publish moves to `/app/game.move` with payload `{ gameId, from, to, promotion }`.
9. Publish chat messages to `/app/game.chat` with payload `{ gameId, message }`.
10. The `/topic/game/{gameId}` subscription currently receives both `GameResponse` payloads and `GameChatMessageResponse` payloads, so implement payload-shape discrimination on the frontend.
11. Replace auth localStorage flow with `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, and `/api/auth/me`.
12. Replace settings localStorage flow with `/api/settings`.
13. Replace leaderboard and profile mock data with `/api/leaderboard` and `/api/profile/*`.
14. Replace local game persistence with `/api/games/*`.
15. Replace local chat with `/api/games/{gameId}/chat` plus WebSocket chat updates.
16. Use the backend response shapes in this README as the frontend contract.
17. Preserve current UI behavior as much as possible, but update data fetching, auth/session handling, and socket handling to match the backend.

Backend caveats to respect:

1. Analysis responses require Stockfish on the backend host for live engine output.
2. Bot games are scaffolded but not true engine-driven play yet.
3. Live FEN/PGN generation is placeholder-quality right now.
4. Leaderboard does not yet support true time-control filtering.
5. Game-state validation is not full engine-grade legality yet.

The frontend task is to integrate with the backend that exists now, not to redesign the backend contract.
