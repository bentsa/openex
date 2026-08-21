#!/bin/bash
ollama serve &
sleep 5
ollama pull mistral:7b-instruct-q4_K_M
python app.py