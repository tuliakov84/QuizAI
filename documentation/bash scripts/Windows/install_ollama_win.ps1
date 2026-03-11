Write-Host "установка ollama..."

# Проверяем WSL
if (-not (wsl.exe --status 2>$null)) {
    Write-Host "WSL2 не установлен. Устанавливаю..."
    wsl --install
    Write-Host "Перезагрузите Windows и запустите снова."
    exit
}

# Запускаем Ubuntu и ставим Ollama через apt
wsl -d Ubuntu -e bash -c "
  sudo apt update &&
  sudo apt install -y curl gpg &&
  curl -fsSL https://ollama.com/download/ollama.gpg | sudo gpg --dearmor -o /usr/share/keyrings/ollama.gpg &&
  echo 'deb [signed-by=/usr/share/keyrings/ollama.gpg] https://ollama.com/apt ./' | sudo tee /etc/apt/sources.list.d/ollama.list &&
  sudo apt update &&
  sudo apt install -y ollama &&
  ollama pull qwen2.5
"

Write-Host "done!"