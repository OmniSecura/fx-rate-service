#!/bin/bash

# rebuild_indexes.sh - Rebuild / optimize tables and indexes for MySQL
#
# Usage: ./rebuild_indexes.sh [OPTIONS]
#
# Options:
#   -d  Database name    (default: fx_rate_db)
#   -m  Mode             (default: auto) [auto|docker|local]
#   -c  Container name   (default: fx-rate-db)
#   -h  Host             (default: localhost)
#   -P  Port             (default: 3306)
#   -u  User             (default: root)
#   -p  Password         (default: rootpassword)
#
# Example:
#   ./rebuild_indexes.sh
#   ./rebuild_indexes.sh -m docker
#   ./rebuild_indexes.sh -m local -h linux-poland14.neueda.com

DB_NAME="fx_rate_db"
CONTAINER="fx-rate-db"
HOST="localhost"
PORT="3306"
USER="root"
PASSWORD="rootpassword"
MODE="auto"

while getopts "d:m:c:h:P:u:p:" opt; do
    case $opt in
        d) DB_NAME="$OPTARG" ;;
        m) MODE="$OPTARG" ;;
        c) CONTAINER="$OPTARG" ;;
        h) HOST="$OPTARG" ;;
        P) PORT="$OPTARG" ;;
        u) USER="$OPTARG" ;;
        p) PASSWORD="$OPTARG" ;;
        *) echo "Unknown flag: -$OPTARG"; exit 1 ;;
    esac
done

# Auto detect mode
if [ "$MODE" == "auto" ]; then
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${CONTAINER}$"; then
        MODE="docker"
    else
        MODE="local"
    fi
fi

if [ "$MODE" == "local" ]; then
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"
    else
        MYSQL="mysql"
    fi
fi

run_mysql() {
    if [ "$MODE" == "docker" ]; then
        docker exec "$CONTAINER" mysql -u "$USER" -p"$PASSWORD" "$DB_NAME"
    else
        "$MYSQL" -h "$HOST" -P "$PORT" -u "$USER" -p"$PASSWORD" "$DB_NAME"
    fi
}

echo "➜ Rebuild indexes / optimize tables"
echo "  Mode:     $MODE"
[ "$MODE" == "docker" ] && echo "  Container: $CONTAINER"
[ "$MODE" == "local"  ] && echo "  Host:      $HOST:$PORT"
echo "  Database: $DB_NAME"
echo ""

START_TIME=$(date +%s)

run_mysql <<EOF
OPTIMIZE TABLE currency;
OPTIMIZE TABLE rate_provider;
OPTIMIZE TABLE currency_pair;
OPTIMIZE TABLE exchange_rate;
OPTIMIZE TABLE eod_fixing;
OPTIMIZE TABLE forward_rate;
OPTIMIZE TABLE rate_alert;
OPTIMIZE TABLE rate_audit_log;
EOF

if [ $? -ne 0 ]; then
    echo "✗ Error: Optimize failed"
    exit 1
fi

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "✓ Index/table maintenance completed successfully"
echo "  Database: $DB_NAME"
echo "  Duration: ${ELAPSED} seconds"