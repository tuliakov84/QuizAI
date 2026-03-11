@echo off
for /F %%a in ('echo prompt $E ^| cmd') do @set "ESC=%%a"
docker compose up -d
docker compose ps
echo.
echo %ESC%[32mPRESS CTRL-C TO QUIT . . .%ESC%[0m
echo.
docker-compose logs -f postgres
