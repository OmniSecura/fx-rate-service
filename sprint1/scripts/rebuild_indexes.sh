#!/bin/bash

# rebuild_indexes.sh - Rebuild / optimize tables and indexes for MySQL
#
# Usage: ./rebuild_indexes.sh [database_name]
#
# Arguments:
#   database_name  Database to optimize (default: fx_rate_db)
#
# Example:
#   ./rebuild_indexes.sh fx_rate_db
#
# Notes:
#   - For MySQL, OPTIMIZE TABLE is used as a practical index/table maintenance step
#   - Safe to use in Git Bash on Windows
#   - Requires MySQL client tools in PATH

set -e

MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"
DB_NAME=${1:-fx_rate_db}

if [ ! -x "$MYSQL" ]; then
    echo "✗ Error: MySQL client not found at: $MYSQL"
    exit 1
fi

echo "➜ Rebuild indexes / optimize tables"
echo "  Database: $DB_NAME"
echo ""

START_TIME=$(date +%s)

"$MYSQL" -u root -p "$DB_NAME" <<EOF
OPTIMIZE TABLE currency;
OPTIMIZE TABLE rate_provider;
OPTIMIZE TABLE currency_pair;
OPTIMIZE TABLE exchange_rate;
OPTIMIZE TABLE eod_fixing;
OPTIMIZE TABLE forward_rate;
OPTIMIZE TABLE rate_alert;
OPTIMIZE TABLE rate_audit_log;
EOF

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "✓ Index/table maintenance completed successfully"
echo "  Database: $DB_NAME"
echo "  Duration: ${ELAPSED} seconds"