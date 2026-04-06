#!/bin/bash

# db_reload.sh - Database reload utility
# Safely drops and recreates database from SQL dump file
#
# Usage: ./db_reload.sh [database_name] [dump_file]
#
# Arguments:
#   database_name  Database to reload (default: fx_rate_db)
#   dump_file      SQL dump file (default: fx_rate_dump.sql)
#
# Example: ./db_reload.sh fx_rate_db fx_rate_dump.sql
#
# How it works:
#   1. Validates dump file exists
#   2. Prompts for MySQL root password (hidden input)
#   3. Drops existing database (or creates if not exists)
#   4. Creates fresh empty database
#   5. Loads complete schema and data from dump file
#   6. Exits on any error (|| exit 1)
#
# Notes:
#   - Password is never stored or logged
#   - Safe to use in Git Bash on Windows
#   - Requires MySQL client tools in PATH

MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"

DB=${1:-fx_rate_db}
DUMP=${2:-fx_rate_dump.sql}

# Validate dump file
if [ ! -f "$DUMP" ]; then
    echo "✗ Error: Dump file not found: $DUMP"
    exit 1
fi

echo "➜ Database reload tool"
echo "  Database: $DB"
echo "  Source:   $DUMP"
echo ""

# Drop and recreate database
echo "⏳ Dropping existing database and creating fresh one..."
"$MYSQL" -u root -p -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB;" || exit 1
echo "✓ Database ready"

# Load data from dump
echo "⏳ Loading schema and data from dump..."
"$MYSQL" -u root -p "$DB" < "$DUMP" || exit 1
echo "✓ Data loaded successfully"

echo ""
echo "✓ Complete - database '$DB' is ready"
