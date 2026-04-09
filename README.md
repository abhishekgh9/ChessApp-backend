# Frontend Handoff: Bot API (Stockfish)

## Scope
This file contains only the frontend-relevant backend changes for bot games.

## What changed for frontend

1. Bot games are now engine-backed (Stockfish).
2. `POST /api/bot-games` still creates a bot game.
3. If player chooses `color = black`, backend immediately plays white (bot) first move.
4. In bot games, `POST /api/games/{gameId}/move` now:
   - validates human move legality on backend,
   - persists human move,
   - auto-plays bot reply,
   - returns updated `GameResponse`.

## Frontend implementation requirements

1. Keep using `POST /api/bot-games` with:
```json
{
  "level": 1,
  "color": "white"
}
```
2. Level range to send: `1..10`.
3. After bot-game create, render returned board directly (if color is black, bot may have already moved).
4. After each human move, always render server response as source of truth.
5. Do not compute bot moves on frontend.
6. Keep using these `GameResponse` fields in bot UI:
   - `isBotGame`
   - `botLevel`
   - `fen`
   - `history`
   - `lastMove`
   - `turnColor`

## Error handling required in frontend

Handle these backend error messages for bot flow:

1. `illegal_move`
2. `stockfish_unavailable`
3. `stockfish_timeout`

## Environment note (for deployment/runtime)

Backend needs Stockfish executable available.

Config used by backend:

```properties
bot.stockfish.path=${BOT_STOCKFISH_PATH:stockfish}
bot.stockfish.command-timeout-ms=${BOT_STOCKFISH_TIMEOUT_MS:5000}
```

If Stockfish is missing, bot endpoints will fail with `stockfish_unavailable`.
