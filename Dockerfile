# syntax=docker/dockerfile:1.7

# ---------- Stage 1: build the React/Vite frontend ----------
FROM node:22-alpine AS frontend
WORKDIR /app/frontend

# Install deps with a clean lockfile-based install when possible.
COPY frontend/package.json frontend/package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY frontend/ ./
RUN npm run build


# ---------- Stage 2: build the Scala backend (with frontend bundle embedded) ----------
FROM eclipse-temurin:21-jdk AS backend

ENV SBT_VERSION=1.10.10
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
 && curl -fsSL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
    | tar -xz -C /opt \
 && ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app/backend

# Cache deps: copy only the build files first so this layer survives most code edits.
COPY backend/project/ ./project/
COPY backend/build.sbt ./
RUN sbt -batch -no-colors update

# Now copy sources + the built frontend bundle into resources.
COPY backend/src/ ./src/
COPY --from=frontend /app/frontend/dist/ ./src/main/resources/public/

RUN sbt -batch -no-colors Universal/stage


# ---------- Stage 3: minimal runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend /app/backend/target/universal/stage/ ./

# Railway sets PORT; we honour it via HTTP_PORT (AppConfig defaults to 8081).
ENV HTTP_PORT=8081
EXPOSE 8081

CMD ["./bin/race-training-backend"]
