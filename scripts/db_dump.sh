#!/bin/bash

# db_dump.sh - Database backup utility
#
# Usage: ./db_dump.sh [OPTIONS]
#
# Options:
#   -d  Database name    (default: fx_rate_db)
#   -o  Output file      (default: fx_rate_dump.sql)
#   -m  Mode             (default: auto) [auto|docker|local]
#   -c  Container name   (default: fx-rate-db)
#   -h  Host             (default: localhost)
#   -P  Port             (default: 3306)
#   -u  User             (default: root)
#   -p  Password         (default: rootpassword)
#
# Examples:
#   ./db_dump.sh                                        # auto detect
#   ./db_dump.sh -m docker                              # force docker
#   ./db_dump.sh -m local                               # force local MySQL
#   ./db_dump.sh -m local -h linux-poland14.neueda.com  # remote MySQL

DB_NAME="fx_rate_db"
OUTPUT_FILE="fx_rate_dump.sql"
CONTAINER="fx-rate-db"
HOST="localhost"
PORT="3306"
USER="root"
PASSWORD="rootpassword"
MODE="auto"

while getopts "d:o:m:c:h:P:u:p:" opt; do
    case $opt in
        d) DB_NAME="$OPTARG" ;;
        o) OUTPUT_FILE="$OPTARG" ;;
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

# Resolve mysqldump path for local mode
if [ "$MODE" == "local" ]; then
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        MYSQLDUMP="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump"
    else
        MYSQLDUMP="mysqldump"
    fi
fi

echo "➜ Database backup tool"
echo "  Mode:        $MODE"
[ "$MODE" == "docker" ] && echo "  Container:   $CONTAINER"
[ "$MODE" == "local"  ] && echo "  Host:        $HOST:$PORT"
echo "  User:        $USER"
echo "  Source:      $DB_NAME"
echo "  Destination: $OUTPUT_FILE"
echo ""

echo "⏳ Creating database dump..."

if [ "$MODE" == "docker" ]; then
    docker exec "$CONTAINER" mysqldump -u "$USER" -p"$PASSWORD" \
        --single-transaction --routines --triggers "$DB_NAME" > "$OUTPUT_FILE"
else
    "$MYSQLDUMP" -h "$HOST" -P "$PORT" -u "$USER" -p"$PASSWORD" \
        --single-transaction --routines --triggers "$DB_NAME" > "$OUTPUT_FILE"
fi

if [ $? -eq 0 ]; then
    SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
    echo "✓ Dump completed successfully"
    echo "  File: $OUTPUT_FILE"
    echo "  Size: $SIZE"
else
    echo "✗ Error: Dump failed"
    exit 1
fi