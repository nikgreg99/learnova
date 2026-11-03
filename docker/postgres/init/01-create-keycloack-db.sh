
#!/bin/bash
set -e

echo "Initializing Keycloak database..."

psql \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 \
    --set=kc_user="$KEYCLOAK_DB_USER" \
    --set=kc_password="$KEYCLOAK_DB_PASSWORD" \
    --set=kc_db="$KEYCLOAK_DB_NAME" <<'SQL'

SELECT format(
    'CREATE ROLE %I WITH LOGIN PASSWORD %L',
    :'kc_user',
    :'kc_password'
)
WHERE NOT EXISTS (
    SELECT FROM pg_roles WHERE rolname = :'kc_user'
)
\gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    :'kc_db',
    :'kc_user'
)
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'kc_db'
)
\gexec

SQL

echo "Keycloak database initialized successfully."
