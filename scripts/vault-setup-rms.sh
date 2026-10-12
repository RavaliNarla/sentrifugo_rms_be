#!/bin/bash
# Run ON THE SERVER after Vault is unsealed and you are logged in with the root token.
# Creates KV path secret/rms/app, AppRole for RMS, and prints role_id / secret_id.
#
# Usage:
#   export VAULT_ADDR='http://127.0.0.1:8200'
#   vault login
#   # edit the REPLACE_ME values below, then:
#   bash vault-setup-rms.sh

set -euo pipefail

: "${VAULT_ADDR:?set VAULT_ADDR e.g. http://127.0.0.1:8200}"

# --- edit these (from New VM Credentials / existing app config) ---
DB_PASSWORD='REPLACE_ME'
JWT_SECRET='REPLACE_ME_AT_LEAST_32_CHARS_LONG_SECRET!!'
MAIL_USERNAME='REPLACE_ME'
MAIL_PASSWORD='REPLACE_ME'
AZURE_STORAGE_CONNECTION_STRING='REPLACE_ME'
# --- end edit ---

if [[ "${DB_PASSWORD}" == "REPLACE_ME" || "${AZURE_STORAGE_CONNECTION_STRING}" == "REPLACE_ME" ]]; then
  echo "Edit REPLACE_ME values in this script before running." >&2
  exit 1
fi

echo "Ensuring KV v2 at secret/ ..."
vault secrets list -format=json 2>/dev/null | grep -q '"secret/"' \
  || vault secrets enable -path=secret kv-v2

echo "Writing secret/rms/app ..."
vault kv put secret/rms/app \
  spring.datasource.password="${DB_PASSWORD}" \
  app.jwt.secret="${JWT_SECRET}" \
  spring.mail.username="${MAIL_USERNAME}" \
  spring.mail.password="${MAIL_PASSWORD}" \
  azure.storage.connection-string="${AZURE_STORAGE_CONNECTION_STRING}"

echo "Writing AppRole policy rms-read ..."
vault policy write rms-read - <<'EOF'
path "secret/data/rms/*" {
  capabilities = ["read"]
}
path "secret/metadata/rms/*" {
  capabilities = ["read", "list"]
}
EOF

echo "Enabling AppRole auth (ok if already enabled) ..."
vault auth enable approle 2>/dev/null || true

echo "Creating AppRole rms-app ..."
vault write auth/approle/role/rms-app \
  token_policies="rms-read" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=0

ROLE_ID=$(vault read -field=role_id auth/approle/role/rms-app/role-id)
SECRET_ID=$(vault write -f -field=secret_id auth/approle/role/rms-app/secret-id)

echo ""
echo "========== SAVE THESE (required for every run) =========="
echo "VAULT_ROLE_ID=${ROLE_ID}"
echo "VAULT_SECRET_ID=${SECRET_ID}"
echo "========================================================"
echo ""
echo "Run example:"
echo "  export VAULT_ROLE_ID=..."
echo "  export VAULT_SECRET_ID=..."
echo "  java -Dspring.profiles.active=azure-dev -jar auth-portal.jar"
