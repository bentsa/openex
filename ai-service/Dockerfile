FROM ollama/ollama:latest AS ollama-base

FROM python:3.11-slim

# Copy Ollama binary from the ollama image
COPY --from=ollama-base /bin/ollama /bin/ollama

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt --break-system-packages || pip install --no-cache-dir -r requirements.txt

COPY . .

ENV OLLAMA_HOST=0.0.0.0
EXPOSE 5001
EXPOSE 11434

COPY start.sh .
RUN chmod +x start.sh

CMD ["./start.sh"]