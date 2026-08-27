FROM ollama/ollama:latest

# Pre-pull the model at build time so the container starts ready to serve,
# matching the model used by ai-service (see docker-compose.yml OLLAMA_MODEL).
RUN (ollama serve &) && sleep 5 && ollama pull qwen2.5:0.5b && pkill ollama

EXPOSE 11434

ENTRYPOINT ["ollama", "serve"]