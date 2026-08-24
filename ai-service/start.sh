#!/bin/bash
python app.py &
ollama serve &
sleep 5
ollama pull qwen2.5:0.5b
wait