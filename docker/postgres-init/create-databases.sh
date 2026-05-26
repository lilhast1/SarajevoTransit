#!/bin/bash
set -e

for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE "$db";
EOSQL
    echo "Created database: $db"
done

# Ensure `loyalty_points_lifetime` exists on the userdb digital_wallets table (idempotent)
if [[ -n "${POSTGRES_MULTIPLE_DATABASES}" ]]; then
    if echo "$POSTGRES_MULTIPLE_DATABASES" | grep -q "userdb"; then
        echo "Ensuring loyalty_points_lifetime column exists in userdb.digital_wallets"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "userdb" <<-EOSQL
            ALTER TABLE digital_wallets ADD COLUMN IF NOT EXISTS loyalty_points_lifetime bigint DEFAULT 0;
        EOSQL
    fi
fi
