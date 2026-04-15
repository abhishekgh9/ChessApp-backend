# ChessMaster Pro Backend

Production-style backend service for ChessMaster Pro, built with Spring Boot, PostgreSQL, JWT authentication, REST APIs, and STOMP WebSocket messaging.

This service provides core capabilities for:

- User authentication and profile management
- Real-time multiplayer game state updates
- Persisted game and chat history
- Matchmaking and leaderboard flows
- Bot game creation and analysis integration (Stockfish-backed)

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Core Features](#2-core-features)
- [3. Technology Stack](#3-technology-stack)
- [4. Architecture](#4-architecture)
- [5. Repository Structure](#5-repository-structure)
- [6. API and Messaging Surface](#6-api-and-messaging-surface)
- [7. Security Model](#7-security-model)
- [8. Configuration](#8-configuration)
- [9. Local Development Setup](#9-local-development-setup)
- [10. Database Initialization](#10-database-initialization)
- [11. Build, Test, and Run](#11-build-test-and-run)
- [12. Operational Guidance](#12-operational-guidance)
- [13. Known Limitations](#13-known-limitations)
- [14. Troubleshooting](#14-troubleshooting)
- [15. Roadmap Suggestions](#15-roadmap-suggestions)
- [16. License and Ownership](#16-license-and-ownership)

## 1. Project Overview

ChessMaster Pro Backend is a stateful backend for a modern chess platform. It is designed to serve a web frontend and support both synchronous API workflows and asynchronous real-time updates.

The service acts as the system of record for:

- Users and credentials
- User preferences/settings
- Games and move history
- In-game chat messages
- Basic profile and leaderboard data

## 2. Core Features

### Authentication and Identity

- JWT-based stateless authentication
- Registration and login flows
- Authenticated user resolution for protected resources

### Game Domain

- Create and retrieve games
- Submit moves, resign, and draw workflow support
- Persisted move history and chat history per game
- PGN/FEN endpoints for game representation retrieval

### Real-Time Messaging

- STOMP-over-WebSocket endpoint for live game events
- Topic broadcast model for game updates and in-game chat
- JWT support in WebSocket connect flow via inbound interceptor

### Matchmaking and Social Surface

- Matchmaking join/cancel/status endpoints
- Leaderboard query endpoint
- Public profile lookup and user game history endpoints

### Engine Integration

- Analysis endpoints for PGN and FEN
- Bot game creation with configurable Stockfish engine path

## 3. Technology Stack

- Java 21
- Spring Boot 4.0.3
- Spring MVC
- Spring Data JPA + Hibernate
- Spring Security
- Spring WebSocket (STOMP broker)
- PostgreSQL
- JWT (JJWT)
- Chesslib
- Maven Wrapper
- JUnit test stack (Spring Boot test starters)

## 4. Architecture

The backend follows a layered Spring architecture:

- Controller layer: HTTP and STOMP entry points
- Service layer: Domain logic and orchestration
- Repository layer: JPA-based persistence
- Security layer: JWT filter + user detail service + CORS policy

High-level flow:

1. Client authenticates through REST endpoints and receives a JWT.
2. Client calls protected APIs with Bearer token.
3. Client connects to WebSocket endpoint with JWT in STOMP connect headers.
4. Domain updates are persisted in PostgreSQL and published to topic subscribers.

## 5. Repository Structure

Top-level layout (trimmed):

```text
.
|- src/main/java/com/chess/demo
|  |- controller      # REST + STOMP handlers
|  |- service         # Business logic
|  |- repository      # JPA repositories
|  |- entity          # Persistence entities
|  |- dto             # Request/response contracts
|  |- security        # JWT and security configuration
|  |- config          # Jackson and WebSocket config
|- src/main/resources
|  |- application.properties
|  |- schema.sql
|- database
|  |- supabase-init.sql
|  |- supabase-seed.sql
|- src/test
|  |- java/com/chess/demo
|- pom.xml
|- mvnw / mvnw.cmd
```

## 6. API and Messaging Surface

### REST Endpoint Groups

- `/api/auth`
	- register, login, logout, current user
- `/api/settings`
	- get and patch user settings
- `/api/games`
	- create game, get game, move, resign, draw flow, pgn/fen retrieval
- `/api/games/{gameId}/chat`
	- chat history + post message
- `/api/bot-games`
	- create bot game
- `/api/analysis`
	- analyze by PGN and FEN
- `/api/puzzles`
	- public listing, detail, daily puzzle, authenticated attempts, and progress summary
- `/api/leaderboard`
	- local app leaderboard query
- `/api/leaderboard/fide`
	- public FIDE leaderboard query with time-control and federation filters
- `/api/profile`
	- own profile (protected) and public profile views
- `/api/matchmaking`
	- join, cancel, and status
- `/api/internal/fide/sync`
	- authenticated manual FIDE import trigger

### WebSocket / STOMP

- Endpoint: `/ws-chess`
- Application destination prefix: `/app`
- Broker topic prefix: `/topic`

Publish destinations:

- `/app/game.move`
- `/app/game.chat`

Subscribe destination:

- `/topic/game/{gameId}`

## 7. Security Model

### Authentication

- JWT bearer tokens secure protected routes.
- A custom JWT authentication filter resolves and validates caller identity.

### Authorization Policy

- Public (permitAll):
	- `/api/auth/**`
	- `/api/leaderboard/**`
	- `/api/profile/*`
	- `/api/profile/*/games`
	- `/api/profile/*/achievements`
- `/api/analysis/**`
- `/api/puzzles`
- `/api/puzzles/daily`
- `/api/puzzles/{id}`
	- `/ws-chess/**`
- Protected:
	- `/api/profile/me`
	- `/api/puzzles/{id}/attempts`
	- `/api/puzzles/me/progress`
	- all other non-public endpoints

### CORS and Session

- CORS allows localhost HTTP/HTTPS origin patterns.
- Stateless session policy (`SessionCreationPolicy.STATELESS`).
- CSRF disabled for token-based API usage.

### Enterprise Security Notes

Before production rollout, enforce the following controls:

1. Remove all credential defaults from property files.
2. Use strong, rotated JWT secrets from a secret manager.
3. Restrict CORS to approved domains only.
4. Introduce rate limiting on auth and gameplay mutation endpoints.
5. Add audit trails for privileged operations and security events.

## 8. Configuration

Key environment-backed properties:

| Property | Purpose | Example / Default |
|---|---|---|
| `SERVER_PORT` | HTTP server port | `8081` |
| `DB_URL` | PostgreSQL JDBC URL | Supabase pooler URL |
| `DB_USERNAME` | Database username | project DB user |
| `DB_PASSWORD` | Database password | required for non-dev |
| `DB_MAX_POOL_SIZE` | Hikari max pool | `10` |
| `DB_MIN_IDLE` | Hikari min idle | `2` |
| `JWT_SECRET` | Base64 JWT signing secret | set per environment |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `3600000` |
| `BOT_STOCKFISH_PATH` | Stockfish executable path | `stockfish` |
| `BOT_STOCKFISH_TIMEOUT_MS` | Engine timeout | `5000` |
| `FIDE_COMBINED_SOURCE` | XML or ZIP resource/URL for combined FIDE list | official latest ZIP |
| `FIDE_STANDARD_SOURCE` | XML or ZIP resource/URL for standard list | unset |
| `FIDE_RAPID_SOURCE` | XML or ZIP resource/URL for rapid list | unset |
| `FIDE_BLITZ_SOURCE` | XML or ZIP resource/URL for blitz list | unset |
| `FIDE_SYNC_ON_STARTUP` | Import real FIDE data on boot only when table is empty | `true` |
| `FIDE_REFRESH_ENABLED` | Enable scheduled monthly FIDE refresh | `true` |
| `FIDE_REFRESH_CRON` | UTC cron for scheduled FIDE refresh | `0 0 6 10 * *` |

Recommended practice: keep all environment-specific values in deployment secret stores or environment configuration, never in source control.

## 9. Local Development Setup

### Prerequisites

- JDK 21+
- Maven (optional if using wrapper)
- PostgreSQL 14+ (or Supabase)
- Stockfish binary available in PATH (for bot/analysis flows)

### Quick Start

1. Clone repository.
2. Configure environment variables for database and JWT.
3. Initialize database schema.
4. Run application with Maven wrapper.

Windows:

```powershell
setx SERVER_PORT 8081
setx DB_URL "jdbc:postgresql://<host>:5432/<db>"
setx DB_USERNAME "<user>"
setx DB_PASSWORD "<password>"
setx JWT_SECRET "<base64-secret>"
setx BOT_STOCKFISH_PATH "stockfish"
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Service base URL:

- `http://localhost:8081`

## 10. Database Initialization

Two bootstrap SQL files are provided:

- `database/supabase-init.sql`: schema, indexes, comments
- `database/supabase-seed.sql`: initial/seed data

Core tables include:

- `users`
- `user_settings`
- `games`
- `game_moves`
- `game_chat_messages`
- `puzzles`
- `puzzle_solution_steps`
- `puzzle_tags`
- `puzzle_attempts`
- `fide_players`
- `achievements`
- `user_achievements`

### Puzzle Seed Data

The Supabase seed script now includes a puzzle catalog for local/demo environments:

- `database/supabase-seed.sql` inserts 20 sample puzzles across `easy`, `medium`, and `hard`
- tag metadata is included for `mate`, `back-rank`, `fork`, `endgame`, `promotion`, `queen`, `rook`, and `knight`
- puzzle attempts are not pre-seeded, so user progress starts clean

If you want puzzle demo data in a fresh environment, run `database/supabase-init.sql` followed by `database/supabase-seed.sql`.

### Puzzle API

Public endpoints:

- `GET /api/puzzles?difficulty=&theme=&page=&size=`
- `GET /api/puzzles/daily`
- `GET /api/puzzles/{id}`

Authenticated endpoints:

- `POST /api/puzzles/{id}/attempts`
- `GET /api/puzzles/me/progress`

Example list request:

```powershell
curl "http://localhost:8081/api/puzzles?difficulty=easy&theme=mate&page=0&size=10"
```

Example list response:

```json
{
  "items": [
    {
      "id": "93000000-0000-0000-0000-000000000001",
      "title": "Queen Lift to d8",
      "description": "White finishes with a direct queen lift.",
      "fen": "6k1/6pp/8/3Q4/8/8/8/6K1 w - - 0 1",
      "difficulty": "easy",
      "primaryTheme": "mate",
      "tags": ["mate", "queen"],
      "maxWrongAttempts": 2,
      "totalSolutionSteps": 1
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 20,
  "totalPages": 2
}
```

Example attempt request:

```powershell
curl -X POST "http://localhost:8081/api/puzzles/93000000-0000-0000-0000-000000000001/attempts" ^
  -H "Authorization: Bearer <token>" ^
  -H "Content-Type: application/json" ^
  -d "{\"move\":\"d5d8\",\"timeSpentSeconds\":7,\"hintsUsed\":0}"
```

Example attempt response:

```json
{
  "attemptId": "f2f5b754-3aa8-4690-8f7b-8f4896d4d54b",
  "puzzleId": "93000000-0000-0000-0000-000000000001",
  "status": "completed",
  "correct": true,
  "completed": true,
  "failed": false,
  "attemptCount": 1,
  "remainingAttempts": 2,
  "solvedSteps": 1,
  "totalSteps": 1,
  "awardedScore": 100,
  "currentStreak": 1,
  "fen": "3Q2k1/6pp/8/8/8/8/8/6K1 b - - 1 1",
  "message": "Puzzle solved."
}
```

Example progress response:

```json
{
  "attemptedCount": 8,
  "solvedCount": 5,
  "successRate": 62.5,
  "currentStreak": 2,
  "bestStreak": 4
}
```

### FIDE Leaderboard Import

The backend can now maintain a separate FIDE-backed leaderboard table.

- Public read endpoint: `GET /api/leaderboard/fide`
- Supported query params: `query`, `timeControl`, `country`, `gender`, `division`, `page`, `size`, `activeOnly`
- Supported `timeControl` values: `standard`, `rapid`, `blitz`
- Supported `gender` values: `open`, `male`, `female`
- Supported `division` values: `open`, `junior`, `senior`

To load official monthly lists, set one or more of:

```powershell
setx FIDE_COMBINED_SOURCE "https://ratings.fide.com/download/players_list_xml.zip"
setx FIDE_STANDARD_SOURCE "https://..."
setx FIDE_RAPID_SOURCE "https://..."
setx FIDE_BLITZ_SOURCE "https://..."
setx FIDE_SYNC_ON_STARTUP "true"
```

The importer accepts XML resources directly and ZIP files containing XML. By default it uses the official combined latest FIDE XML ZIP, which includes standard, rapid, and blitz ratings in one source.

Sync behavior:

- on startup, import runs only if the `fide_players` table is empty
- after that, a scheduled monthly refresh runs by default on the 10th day of each month at `06:00 UTC`
- manual sync remains available through the internal endpoint

Manual sync is also available after login:

```powershell
curl -X POST "http://localhost:8081/api/internal/fide/sync?timeControl=all" ^
  -H "Authorization: Bearer <token>"
```

For Supabase, run SQL scripts in the SQL editor for your target project.

## 11. Build, Test, and Run

### Build

```powershell
.\mvnw.cmd clean package
```

### Test

```powershell
.\mvnw.cmd test
```

### Run Packaged Jar

```powershell
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Health and Diagnostics

Actuator is included. Validate exposed endpoints and secure sensitive actuator routes before production use.

## 12. Operational Guidance

### Environment Promotion Model

- Dev: local PostgreSQL or isolated Supabase project
- Staging: production-like settings and data volumes
- Production: hardened secrets, strict CORS, monitoring, backups

### Observability Recommendations

1. Centralized structured logging (request ID correlation).
2. Metrics scraping and dashboarding (latency, error rate, throughput).
3. Alerting for auth failures, DB saturation, and WebSocket disconnect spikes.

### Reliability Recommendations

1. Use connection pool sizing tied to DB capacity.
2. Configure graceful shutdown and readiness probes.
3. Externalize matchmaking state if scaling beyond single-instance runtime.

## 13. Known Limitations

Current implementation caveats:

1. Some game legality/state transitions are not yet full engine-grade validation.
2. FEN and PGN representations during live play can be placeholder-quality.
3. Matchmaking is currently in-memory and not horizontally scalable.
4. WebSocket topic payloads can contain mixed message shapes without a formal event envelope.
5. The local app leaderboard remains basic; the separate FIDE leaderboard depends on imported official list data.
6. Puzzle scoring and streaks are intentionally simple for the MVP: they track solve/fail outcomes, first-try bonuses, and hint penalties but do not yet use rating-based difficulty.
7. Puzzle solution authoring supports multi-step lines, but the current seed catalog is mostly single-step tactics.

These limitations are suitable for active development but should be addressed for full production hardening.

## 14. Troubleshooting

### Application fails to connect to database

- Verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Confirm network access, SSL mode, and PostgreSQL host allowlist.

### JWT authentication issues

- Confirm `JWT_SECRET` is valid Base64 and consistent across restarts.
- Ensure client sends `Authorization: Bearer <token>`.

### WebSocket connect or subscription failures

- Verify endpoint `/ws-chess` and origin policy.
- Confirm JWT is included in STOMP CONNECT headers.

### Bot/analysis failures

- Confirm Stockfish executable is reachable via `BOT_STOCKFISH_PATH`.
- Increase `BOT_STOCKFISH_TIMEOUT_MS` if engine response is timing out.

## 15. Roadmap Suggestions

Priority improvements for enterprise readiness:

1. Introduce typed WebSocket event envelopes (`type`, `payload`, `version`).
2. Implement full chess move legality and canonical FEN/PGN generation.
3. Move matchmaking from in-memory to distributed coordination/store.
4. Add OpenAPI specification and API versioning strategy.
5. Implement token revocation/refresh flow and session management controls.
6. Add contract tests and load tests for gameplay and WebSocket paths.
7. Add CI security scanning, dependency audit, and SBOM generation.

## 16. License and Ownership

No explicit license file is currently defined in this repository.

For enterprise use, add:

1. A clear LICENSE file.
2. CODEOWNERS and contribution standards.
3. Security policy and vulnerability disclosure process.

