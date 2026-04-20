#!/bin/bash

# db_reload.sh - Database reload utility
#
# Usage: ./db_reload.sh [OPTIONS]
#
# Options:
#   -d  Database name    (default: fx_rate_db)
#   -f  Dump file        (default: fx_rate_dump.sql)
#   -m  Mode             (default: auto) [auto|docker|local]
#   -c  Container name   (default: fx-rate-db)
#   -h  Host             (default: localhost)
#   -P  Port             (default: 3306)
#   -u  User             (default: root)
#   -p  Password         (default: rootpassword)

DB="fx_rate_db"
DUMP="fx_rate_dump.sql"
CONTAINER="fx-rate-db"
HOST="localhost"
PORT="3306"
USER="root"
PASSWORD="rootpassword"
MODE="auto"

while getopts "d:f:m:c:h:P:u:p:" opt; do
    case $opt in
        d) DB="$OPTARG" ;;
        f) DUMP="$OPTARG" ;;
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

# Resolve mysql path for local mode
if [ "$MODE" == "local" ]; then
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"
    else
        MYSQL="mysql"
    fi
fi

if [ ! -f "$DUMP" ]; then
    echo "✗ Error: Dump file not found: $DUMP"
    exit 1
fi

echo "➜ Database reload tool"
echo "  Mode:      $MODE"
[ "$MODE" == "docker" ] && echo "  Container: $CONTAINER"
[ "$MODE" == "local"  ] && echo "  Host:      $HOST:$PORT"
echo "  User:      $USER"
echo "  Database:  $DB"
echo "  Source:    $DUMP"
echo ""

echo "⏳ Dropping and recreating database..."
if [ "$MODE" == "docker" ]; then
    docker exec "$CONTAINER" mysql -u "$USER" -p"$PASSWORD" \
        -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB;" || exit 1
else
    "$MYSQL" -h "$HOST" -P "$PORT" -u "$USER" -p"$PASSWORD" \
        -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB;" || exit 1
fi
echo "✓ Database ready"

echo "⏳ Loading schema and data from dump..."
if [ "$MODE" == "docker" ]; then
    docker exec -i "$CONTAINER" mysql -u "$USER" -p"$PASSWORD" "$DB" < "$DUMP" || exit 1
else
    "$MYSQL" -h "$HOST" -P "$PORT" -u "$USER" -p"$PASSWORD" "$DB" < "$DUMP" || exit 1
fi
echo "✓ Data loaded successfully"

echo ""
echo "✓ Complete - database '$DB' is ready"