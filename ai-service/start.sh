#!/bin/bash
python app.py &
ollama serve &
sleep 5
ollama pull mistral:7b-instruct-q4_K_M
wait