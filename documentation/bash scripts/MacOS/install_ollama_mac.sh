#!/bin/bash
set -e
#Внимание! При запуске скрипта будет установлен Homebrew, если он не установлен.
if ! command -v brew &>/dev/null; then
  echo "Homebrew не найден. Устанавливаю..."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
  eval "$(/opt/homebrew/bin/brew shellenv)"
fi

brew install ollama

echo "установка qwen2.5..."
ollama pull qwen2.5

echo "done!"