#!/bin/bash

# db_dump.sh - Database backup utility
# Creates full SQL dump of schema and data
#
# Usage: ./db_dump.sh [database_name] [output_file]
#
# Arguments:
#   database_name  Database to dump (default: fx_rate_db)
#   output_file    Output SQL file (default: fx_rate_dump.sql)
#
# Example: ./db_dump.sh fx_rate_db fx_rate_dump.sql
#
# How it works:
#   1. Prompts for MySQL root password (hidden input)
#   2. Dumps complete schema, data, routines and triggers
#   3. Uses single transaction for consistency
#   4. Saves to output file
#   5. Confirms completion with file info
#
# Notes:
#   - Password is never stored or logged
#   - Safe to use in Git Bash on Windows
#   - Requires MySQL client tools in PATH

MYSQLDUMP="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump"

DB_NAME=${1:-fx_rate_db}
OUTPUT_FILE=${2:-fx_rate_dump.sql}

echo "➜ Database backup tool"
echo "  Source:      $DB_NAME"
echo "  Destination: $OUTPUT_FILE"
echo ""

echo "⏳ Creating database dump..."
"$MYSQLDUMP" -u root -p --routines --triggers --single-transaction "$DB_NAME" > "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
    echo "✓ Dump completed successfully"
    echo "  File: $OUTPUT_FILE"
    echo "  Size: $SIZE"
else
    echo "✗ Error: Dump failed"
    exit 1
fi
