#!/bin/bash

docker compose up -d
docker compose ps
echo
echo -e "\033[32mPRESS CTRL-C TO QUIT . . .\033[0m"
echo
docker-compose logs -f postgres