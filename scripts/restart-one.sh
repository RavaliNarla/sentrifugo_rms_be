#!/bin/bash
# Restart one RMS portal on Sentrifugo-RM1. Requires VAULT_ROLE_ID + VAULT_SECRET_ID.
set -e
NAME="$1"
PORT="$2"
JAR="/opt/app-jars/${NAME}.jar"
LOG="/opt/app-jars/logs/${NAME}.log"

: "${VAULT_ROLE_ID:?set VAULT_ROLE_ID}"
: "${VAULT_SECRET_ID:?set VAULT_SECRET_ID}"

sudo pkill -f "${JAR}" || true
sleep 6
sudo bash -c ": > '${LOG}'"
sudo bash -c "cd /opt/app-jars && VAULT_ROLE_ID='${VAULT_ROLE_ID}' VAULT_SECRET_ID='${VAULT_SECRET_ID}' nohup java -Xms128m -Xmx768m -Dspring.profiles.active=azure-dev -jar '${JAR}' --server.port='${PORT}' >>'${LOG}' 2>&1 & echo \$! > '/opt/app-jars/logs/${NAME}.pid'"
echo "started ${NAME} port=${PORT}"
