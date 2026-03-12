#!/bin/bash
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
docker compose up -d
docker compose ps
echo
echo -e "\033[32mPRESS CTRL-C TO QUIT . . .\033[0m"
echo
docker-compose logs -f postgres