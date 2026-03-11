Write-Host "запуск ollama..."

wsl -d Ubuntu -e ollama serve &
Start-Sleep -Seconds 2

Write-Host "загрузка qwen2.5..."
wsl -d Ubuntu -e ollama run qwen2.5