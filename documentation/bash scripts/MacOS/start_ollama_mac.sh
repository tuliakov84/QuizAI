#!/bin/bash
echo "запуск ollama..."

ollama serve &
sleep 2

echo "запуск qwen 2.5..."
ollama run qwen2.5