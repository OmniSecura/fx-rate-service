#!/bin/bash

# db_dump.sh: Full schema and data dump for MySQL database
# Usage: ./db_dump.sh [database_name] [output_file]
# Defaults: database_name=fx_rate_service, output_file=fx_rate_dump.sql

MYSQLDUMP_CMD="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump"

DB_NAME=${1:-fx_market_data}
OUTPUT_FILE=${2:-fx_rate_dump.sql}

echo "Dumping full schema and data from database '$DB_NAME' to '$OUTPUT_FILE'..."

"$MYSQLDUMP_CMD" -u root -p --routines --triggers --single-transaction "$DB_NAME" > "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    echo "Dump completed successfully: $OUTPUT_FILE"
else
    echo "Error: Dump failed."
    exit 1
fi
