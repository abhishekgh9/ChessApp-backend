# AGENTS.md

This file helps coding agents become productive quickly in this repository.

## Scope
- Backend-only Spring Boot service for ChessMaster Pro.
- Make code changes in source paths, not generated output.
- Follow existing package boundaries and naming conventions.

## First Commands To Run (Windows)
- Build: `./mvnw.cmd clean package`
- Test: `./mvnw.cmd test`
- Run locally: `./mvnw.cmd spring-boot:run`

## Project Map
- Entry point: [src/main/java/com/chess/demo/Application.java](src/main/java/com/chess/demo/Application.java)
- Controllers: [src/main/java/com/chess/demo/controller](src/main/java/com/chess/demo/controller)
- Services: [src/main/java/com/chess/demo/service](src/main/java/com/chess/demo/service)
- Repositories: [src/main/java/com/chess/demo/repository](src/main/java/com/chess/demo/repository)
- Entities: [src/main/java/com/chess/demo/entity](src/main/java/com/chess/demo/entity)
- Security config and JWT flow: [src/main/java/com/chess/demo/security](src/main/java/com/chess/demo/security)
- App config: [src/main/resources/application.properties](src/main/resources/application.properties)
- DB bootstrap scripts: [database/supabase-init.sql](database/supabase-init.sql), [database/supabase-seed.sql](database/supabase-seed.sql)

## Security And API Conventions
- JWT bearer auth protects non-public routes.
- Public route policy is defined in [src/main/java/com/chess/demo/security/SecurityConfig.java](src/main/java/com/chess/demo/security/SecurityConfig.java).
- Keep session model stateless and consistent with existing filter chain.
- For endpoint behavior and route inventory, prefer [README.md](README.md#6-api-and-messaging-surface) and [README.md](README.md#7-security-model).

## Testing Conventions
- Main smoke test: [src/test/java/com/chess/demo/ApplicationTests.java](src/test/java/com/chess/demo/ApplicationTests.java)
- Service test examples: [src/test/java/com/chess/demo/service](src/test/java/com/chess/demo/service)
- After backend changes, run `./mvnw.cmd test`.
- Prefer adding/updating focused tests near changed service/controller logic.

## Environment And Pitfalls
- Do not rely on default secrets in [src/main/resources/application.properties](src/main/resources/application.properties); use env vars in real environments.
- Bot/analysis features require Stockfish (`BOT_STOCKFISH_PATH`).
- FIDE import may run on startup when table is empty (`FIDE_SYNC_ON_STARTUP`).
- Never edit generated build artifacts under [target](target).

## Docs To Link Instead Of Duplicating
- Full architecture and operational guide: [README.md](README.md)
- Frontend/FIDE API handoff: [FRONTEND_API_HANDOFF.md](FRONTEND_API_HANDOFF.md), [FIDE_FRONTEND_HANDOFF_README.md](FIDE_FRONTEND_HANDOFF_README.md)
- Spring template help: [HELP.md](HELP.md)

## Agent Working Style For This Repo
- Keep changes minimal and localized.
- Preserve API contracts unless the task explicitly requires a breaking change.
- If changing security/auth, mention route impact and test impact.
- If changing DB entities/mappings, verify startup and relevant repository/service tests.
