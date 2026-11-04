#!/bin/sh
# Executado UMA vez pelo container oficial do PostgreSQL, na primeira inicializacao (volume vazio).
# Cria um banco por microsservico (Database per Service, ADR-0002).
# As tabelas NAO sao criadas aqui: cada servico aplica suas proprias migrations Flyway ao iniciar.
set -eu

for database in customer_db product_db inventory_db order_db; do
  echo "Creating database ${database}"
  psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" \
    -c "CREATE DATABASE ${database};"
done
