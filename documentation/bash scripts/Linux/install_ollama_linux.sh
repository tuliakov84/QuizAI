#!/bin/bash
set -e

echo "установка ollama..."

sudo apt update

sudo apt install -y curl gpg
curl -fsSL https://ollama.com/download/ollama.gpg | sudo gpg --dearmor -o /usr/share/keyrings/ollama.gpg

echo "deb [signed-by=/usr/share/keyrings/ollama.gpg] https://ollama.com/apt ./" | \
  sudo tee /etc/apt/sources.list.d/ollama.list


sudo apt update
sudo apt install -y ollama

echo "загрузка qwen 2.5"
ollama pull qwen2.5

echo "done!"