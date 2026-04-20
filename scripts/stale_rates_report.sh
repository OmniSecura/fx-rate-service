#!/bin/bash

# stale_rates_report.sh - Report currency pairs not updated within threshold
#
# Usage: ./stale_rates_report.sh [OPTIONS]
#
# Options:
#   -t  Threshold hours  (default: 4)
#   -m  Mode             (default: auto) [auto|docker|local]
#   -c  Container name   (default: fx-rate-db)
#   -h  Host             (default: localhost)
#   -P  Port             (default: 3306)
#   -u  User             (default: root)
#   -p  Password         (default: rootpassword)
#
# Example:
#   ./stale_rates_report.sh
#   ./stale_rates_report.sh -t 2
#   ./stale_rates_report.sh -m docker -t 8

HOURS="4"
CONTAINER="fx-rate-db"
HOST="localhost"
PORT="3306"
USER="root"
PASSWORD="rootpassword"
MODE="auto"
DB_NAME="fx_rate_db"

while getopts "t:m:c:h:P:u:p:" opt; do
    case $opt in
        t) HOURS="$OPTARG" ;;
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

echo "➜ Stale rates report"
echo "  Mode:      $MODE"
[ "$MODE" == "docker" ] && echo "  Container: $CONTAINER"
[ "$MODE" == "local"  ] && echo "  Host:      $HOST:$PORT"
echo "  Threshold: ${HOURS} hours"
echo ""

QUERY="
SELECT
    pair_code AS pair,
    pair_type AS type,
    mid_rate,
    rate_timestamp AS last_updated,
    CONCAT(
        TIMESTAMPDIFF(DAY, rate_timestamp, NOW()), 'd ',
        MOD(TIMESTAMPDIFF(HOUR, rate_timestamp, NOW()), 24), 'h ',
        MOD(TIMESTAMPDIFF(MINUTE, rate_timestamp, NOW()), 60), 'm'
    ) AS age
FROM latest_rates_vw
WHERE hours_since_update > ${HOURS}
ORDER BY rate_timestamp ASC;"

if [ "$MODE" == "docker" ]; then
    docker exec "$CONTAINER" mysql -u "$USER" -p"$PASSWORD" "$DB_NAME" -e "$QUERY"
else
    "$MYSQL" -h "$HOST" -P "$PORT" -u "$USER" -p"$PASSWORD" "$DB_NAME" -e "$QUERY"
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Report completed"
else
    echo "✗ Error: Report failed"
    exit 1
fi