# Race Training Program

Personal site for tracking race training plans and reconciling them against actual workouts pulled from Strava (with .FIT upload as a fallback). v1 target: **Leadville Trail Marathon, 2026-06-27**.

## Stack

- **Backend:** Scala 3 + http4s + doobie + Tapir, Postgres + Flyway, deployed via Docker
- **Frontend:** React + Vite + TypeScript + Tailwind, types generated from Tapir's OpenAPI
- **Hosting:** Railway (single service: Scala server also serves the SPA bundle)

## Quick start

```bash
cp .env.example .env                  # fill in Strava creds when ready
make db-up                            # start local Postgres
cd backend && sbt run                 # backend on :8080 (runs Flyway on boot)
cd frontend && npm install && npm run dev   # SPA on :5173, proxies /api → :8080
```

Open http://localhost:5173.

## Layout

```
backend/    # Scala 3 / sbt
frontend/   # React + Vite + TS
```

See `/Users/ericmetcalf/.claude/plans/i-want-to-build-cosmic-reef.md` for the full architecture plan.
