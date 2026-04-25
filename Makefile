.PHONY: help db-up db-down db-logs psql backend frontend dev build regen-api fmt clean

# Force a modern JDK (Flyway 10 requires Java 17+; Homebrew sbt wrapper
# defaults to a stale openjdk@15 install). Override with JAVA_HOME=... if needed.
export JAVA_HOME ?= $(shell /usr/libexec/java_home 2>/dev/null)

# Prefer the standalone sbt 1.10.10 install at ~/.local/sbt — the Homebrew
# wrapper at /usr/local/bin/sbt loads JNA 5.5.0 (x86_64-only) which crashes
# the boot launcher on Apple Silicon.
SBT := $(shell test -x $(HOME)/.local/sbt/bin/sbt && echo $(HOME)/.local/sbt/bin/sbt || echo sbt)

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

db-up: ## Start local Postgres
	docker compose up -d postgres

db-down: ## Stop local Postgres
	docker compose down

db-logs: ## Tail Postgres logs
	docker compose logs -f postgres

psql: ## Open psql against local DB
	docker compose exec postgres psql -U training -d training

backend: ## Run backend (sbt reStart for hot reload)
	cd backend && $(SBT) ~reStart

frontend: ## Run frontend dev server
	cd frontend && npm run dev

regen-api: ## Regenerate frontend API types from running backend
	cd frontend && ./scripts/regen-api.sh

build: ## Production build (frontend → backend resources, sbt assembly)
	cd frontend && npm run build
	rm -rf backend/src/main/resources/public
	mkdir -p backend/src/main/resources/public
	cp -R frontend/dist/* backend/src/main/resources/public/
	cd backend && $(SBT) Docker/publishLocal

fmt: ## Format Scala + frontend
	cd backend && $(SBT) scalafmtAll
	cd frontend && npm run fmt

clean:
	cd backend && $(SBT) clean
	rm -rf frontend/dist frontend/node_modules backend/src/main/resources/public
