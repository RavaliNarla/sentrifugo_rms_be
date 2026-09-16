#!/usr/bin/env bash
# Start all three RMS portals on the bobdev Linux host (java -jar).
# Usage:
#   export DB_PASSWORD='...'
#   ./scripts/run-azure-dev.sh
#
# Expects fat jars already built:
#   mvn -DskipTests clean package
# and copied next to this script's repo root under each module's target/.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROFILE=azure-dev
HEAP="-Xms128m -Xmx768m"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: set DB_PASSWORD before running." >&2
  exit 1
fi
if [[ -z "${SSL_KEYSTORE_PASSWORD:-}" ]]; then
  echo "ERROR: set SSL_KEYSTORE_PASSWORD (same as BOB_JAVA vault v-ssl-pass / keystore pass)." >&2
  exit 1
fi

mkdir -p /opt/app-jars/uploads/recruiter-portal /opt/app-jars/uploads/master-portal /opt/app-jars/logs

start_one() {
  local name="$1"
  local jar="${JAR_DIR:-/opt/app-jars}/$name.jar"
  if [[ ! -f "$jar" ]]; then
    echo "ERROR: missing $jar" >&2
    exit 1
  fi
  echo "Starting $name ..."
  DB_PASSWORD="$DB_PASSWORD" SSL_KEYSTORE_PASSWORD="$SSL_KEYSTORE_PASSWORD" \
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