# OpenEx 3.0 - Simulated Crypto Exchange & AI Trading Terminal

A lightweight, simulated crypto exchange built with a 100% open-source
microservices architecture: a Kotlin/Spring Boot core (ledger, matching
engine, WebSockets), a React/Vite trading terminal, and a Python/Flask
service powering a local, air-gapped AI trading assistant via Ollama.

## Architecture

| Service      | Stack                              | Port  |
|--------------|-------------------------------------|-------|
| backend/     | Kotlin, Spring Boot, PostgreSQL     | 8080  |
| frontend/    | React, Vite, WebSockets             | 5173  |
| ai-service/  | Python, Flask, LangChain, Ollama    | 5001  |
| PostgreSQL   | Docker                              | 5432  |
| Redis        | Docker                              | 6379  |
| Ollama       | Local LLM runtime (mistral)         | 11434 |

## Prerequisites

- Docker Desktop must be running before starting the backend
  (Postgres/Redis run as containers; the backend fails with a
  Connection refused error on port 5432 if Docker isn't up)
- Java 21 (Eclipse Temurin recommended)
- Node.js (for the frontend)
- Python 3.11+ with a virtual environment
- Ollama installed locally, with the mistral model pulled:
  ollama pull mistral

## Running from a cold start

Start services in this order.

### 1. Infrastructure (Postgres + Redis)
cd openex
docker compose up -d
docker ps

Wait for both containers to show "healthy" before continuing.

### 2. Backend (Kotlin/Spring Boot)
cd backend
.\gradlew.bat bootRun

Wait for "Started OpenExCoreApplicationKt" before continuing.
Runs on http://localhost:8080

### 3. Ollama
ollama list

### 4. AI Service (Python/Flask)
cd ai-service
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py

Runs on http://localhost:5001

### 5. Frontend (React/Vite)
cd frontend
npm install
npm run dev

Runs on http://localhost:5173

## API Overview

### Auth
- POST /api/auth/login - returns a JWT bearer token

### Wallets (Kotlin, requires JWT)
- GET /api/wallets - list the authenticated user's account balances
- POST /api/wallets/deposit - credit a simulated deposit

### AI Chat (Python, requires JWT)
- POST /api/chat - conversational endpoint for the trading assistant

## AI Trading Assistant - Engineering Notes

The /api/chat endpoint uses a local mistral model via Ollama to answer
general trading questions and to retrieve and quote the user's real wallet
balance from the Kotlin ledger.

Design note: the original plan was to register the wallet lookup as a
LangChain tool and let the LLM decide autonomously when to call it via
create_agent. In testing, mistral's native tool-calling support through
LangChain proved unreliable and caused the agent to hang indefinitely.
Given the project deadline, this was replaced with a lightweight
keyword-based dispatch: if the incoming message contains balance-related
terms, the endpoint calls the Kotlin /api/wallets API directly, and the
real data is injected into the prompt before the LLM generates its
response. The LLM still produces the final natural-language answer and
is instructed never to invent numbers, but the decision of whether to
fetch wallet data is made in Python rather than by the model itself.

This was a deliberate scope trade-off under time pressure. A follow-up
would be to try a model with more robust native tool-calling support,
or to debug the LangChain agent hang with more time available.

## DevOps & Containerization

**Containerized (via `docker-compose.yml`):**
- PostgreSQL - with a `pg_isready` healthcheck
- Redis - with a `redis-cli ping` healthcheck

Bring both up with:
```
docker compose up -d
```
Both must report `(healthy)` (check with `docker ps`) before starting
the backend, since the backend has no built-in retry/wait logic for the
database connection.

**Run via local dev servers (not containerized):**
- Kotlin/Spring Boot backend - `./gradlew bootRun`
- Python/Flask AI service - `python app.py` (inside the `venv`)
- React/Vite frontend - `npm run dev`

**Why these three aren't containerized:** the original plan was to
containerize the full stack, including the backend, AI service, and
Ollama, so the entire application could start with a single
`docker-compose up`. In practice this hit two separate blockers close
to the submission deadline:

1. **Backend image build failures.** The Gradle build inside the Docker
   image failed with dependency-resolution and TLS errors when fetching
   plugins/dependencies from within the container's network context - a
   different failure each time the build was retried, suggesting an
   unstable network/proxy environment inside the container rather than
   a fixable configuration issue.
2. **AI service image build failures.** `pip install` timed out
   repeatedly part-way through installing the (fairly heavy)
   LangChain/numpy/pandas dependency set inside the container.

Given limited time before submission, the pragmatic call was to keep
Postgres and Redis containerized (satisfying the core "database as a
container with healthchecks" requirement) and continue running the
backend, AI service, and frontend via their normal dev-server commands,
which are fast, reliable, and already well-tested throughout this
project. A clean follow-up would be to debug the container
network/proxy issue directly (likely a corporate/local DNS or MTU issue
affecting `apt`/`pip`/Gradle registry access from inside containers)
and finish full containerization with more time available.

## Testing

### Backend
cd backend
.\gradlew.bat test

### AI Service
cd ai-service
.\venv\Scripts\Activate.ps1
python -m pytest test_app.py -v

Covers: health check, request validation, correct wallet-data fetch and
prompt injection when a balance question is asked, correct skipping of
the wallet fetch for unrelated questions, and graceful handling when no
auth token is provided.

## Troubleshooting

- Connection refused on port 5432 when starting the backend: Docker
  Desktop isn't running, or the Postgres container isn't up yet.
- 403 on an authenticated endpoint: usually a stale/expired JWT.
  Re-run the login request to get a fresh token.
- /api/chat returns a generic "unable to fetch" error: confirm the
  Kotlin backend is running and reachable at http://localhost:8080.