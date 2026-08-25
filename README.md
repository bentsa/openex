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

- Docker Desktop must be running

That's it. The entire stack — Postgres, Redis, Ollama, the Kotlin backend,
the Python AI service, and the React frontend — now runs in containers via
`docker-compose.yml`. No local Java, Node, or Python installation is
required to run the app; Docker handles all of that inside each service's
image.

## Running from a cold start

From the project root: