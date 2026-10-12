#!/usr/bin/env bash
# Start all three RMS portals on Sentrifugo-RM1 (java -jar).
# Usage:
#   export VAULT_ROLE_ID='...'
#   export VAULT_SECRET_ID='...'
#   ./scripts/run-azure-dev.sh

set -euo pipefail

PROFILE=azure-dev
HEAP="-Xms128m -Xmx768m"

if [[ -z "${VAULT_ROLE_ID:-}" ]]; then
  echo "ERROR: set VAULT_ROLE_ID before running." >&2
  exit 1
fi
if [[ -z "${VAULT_SECRET_ID:-}" ]]; then
  echo "ERROR: set VAULT_SECRET_ID before running." >&2
  exit 1
fi

mkdir -p /opt/app-jars/logs

start_one() {
  local name="$1"
  local jar="${JAR_DIR:-/opt/app-jars}/$name.jar"
  if [[ ! -f "$jar" ]]; then
    echo "ERROR: missing $jar" >&2
    exit 1
  fi
  echo "Starting $name ..."
  VAULT_ROLE_ID="$VAULT_ROLE_ID" VAULT_SECRET_ID="$VAULT_SECRET_ID" \
    nohup java $HEAP \
    -Dspring.profiles.active="$PROFILE" \
    -jar "$jar" \
    >"/opt/app-jars/logs/$name.log" 2>&1 &
  echo $! >"/opt/app-jars/logs/$name.pid"
  echo "  pid=$(cat /opt/app-jars/logs/$name.pid)  log=/opt/app-jars/logs/$name.log"
}

start_one auth-portal
start_one master-portal
start_one recruiter-portal

echo "All portals started with profile=$PROFILE"
