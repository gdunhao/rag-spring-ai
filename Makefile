.PHONY: up down restart logs run build setup pull-models

# ── Docker helpers ─────────────────────────────────────────────────────────

## Start all infrastructure services (postgres + ollama) in the background
up:
	docker compose up -d --remove-orphans

## Stop and remove containers (data volumes are preserved)
down:
	docker compose down

## Restart all services
restart: down up

## Stream logs from all services
logs:
	docker compose logs -f

## Tail logs for a specific service, e.g.: make logs-ollama | make logs-postgres
logs-%:
	docker compose logs -f $*

# ── Model management ───────────────────────────────────────────────────────

## Manually pull Ollama models (use if ollama-init container failed)
pull-models:
	docker exec rag-ollama ollama pull qwen3:4b
	docker exec rag-ollama ollama pull nomic-embed-text

## List models currently available in the running Ollama instance
list-models:
	docker exec rag-ollama ollama list

# ── Application ────────────────────────────────────────────────────────────

## Run the Spring Boot application (infra must already be up)
run:
	./mvnw spring-boot:run

## Build the jar (skip tests)
build:
	./mvnw clean package -DskipTests

# ── Full setup ─────────────────────────────────────────────────────────────

## First-time setup: start infrastructure, wait for models to download
setup: up
	@echo "Infrastructure starting up..."
	@echo "Waiting for Ollama to become healthy (this may take ~30 s)..."
	@until docker inspect --format='{{.State.Health.Status}}' rag-ollama 2>/dev/null | grep -q healthy; do \
		printf '.'; sleep 3; \
	done
	@echo ""
	@echo "Pulling AI models (first run may take several minutes)..."
	docker compose logs -f ollama-init
	@echo ""
	@echo "Setup complete!  Run 'make run' to start the application."

