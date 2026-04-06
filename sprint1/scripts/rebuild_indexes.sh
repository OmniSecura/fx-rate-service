#!/bin/bash

# rebuild_indexes.sh: Rebuild indexes for MySQL database
# Usage: ./rebuild_indexes.sh [database_name]
# Default: database_name=fx_market_data

MYSQL_CMD="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"

DB_NAME=${1:-fx_market_data}

echo "Rebuilding indexes for database '$DB_NAME'..."

START_TIME=$(date +%s)

"$MYSQL_CMD" -u root -p "$DB_NAME" <<EOF

OPTIMIZE TABLE exchange_rate;
OPTIMIZE TABLE currency_pair;
OPTIMIZE TABLE rate_provider;
OPTIMIZE TABLE currency;
OPTIMIZE TABLE eod_fixing;

EOF

if [ $? -eq 0 ]; then
    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))
    echo "Index rebuild completed successfully in ${ELAPSED} seconds."
else
    echo "Error: Index rebuild failed."
    exit 1
fi