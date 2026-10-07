#!/bin/bash
# Restart one RMS portal on Sentrifugo-RM1. Requires DB_PASSWORD in the environment.
set -e
NAME="$1"
PORT="$2"
JAR="/opt/app-jars/${NAME}.jar"
LOG="/opt/app-jars/logs/${NAME}.log"

: "${DB_PASSWORD:?set DB_PASSWORD}"

sudo pkill -f "${JAR}" || true
sleep 6
sudo bash -c ": > '${LOG}'"
sudo bash -c "cd /opt/app-jars && DB_PASSWORD='${DB_PASSWORD}' nohup java -Xms128m -Xmx768m -Dspring.profiles.active=azure-dev -jar '${JAR}' --server.port='${PORT}' >>'${LOG}' 2>&1 & echo \$! > '/opt/app-jars/logs/${NAME}.pid'"
echo "started ${NAME} port=${PORT}"
