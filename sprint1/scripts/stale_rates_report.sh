#!/bin/bash

# stale_rates_report.sh - Stale rates report utility
# Lists all currency pairs not updated in the last N hours
#
# Usage: ./stale_rates_report.sh [hours]
#
# Arguments:
#   hours  Stale threshold in hours (default: 4)
#
# Example: ./stale_rates_report.sh 4
#
# Notes:
#   - Safe to use in Git Bash on Windows
#   - Requires MySQL client tools in PATH

MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"

HOURS=${1:-4}

echo "➜ Stale rates report"
echo "  Threshold: ${HOURS} hours"
echo ""

"$MYSQL" -u root -p fx_rate_db <<EOF2

SELECT
    cp.pair_code                                        AS pair,
    cp.pair_type                                        AS type,
    er.mid_rate                                         AS mid_rate,
    er.rate_timestamp                                   AS last_updated,
    CONCAT(
        TIMESTAMPDIFF(DAY,    er.rate_timestamp, NOW()), 'd ',
        MOD(TIMESTAMPDIFF(HOUR,   er.rate_timestamp, NOW()), 24), 'h ',
        MOD(TIMESTAMPDIFF(MINUTE, er.rate_timestamp, NOW()), 60), 'm'
    )                                                   AS age
FROM currency_pair cp
JOIN exchange_rate er
  ON er.rate_id = (
      SELECT rate_id
      FROM   exchange_rate
      WHERE  pair_id  = cp.pair_id
        AND  is_valid = 1
      ORDER  BY rate_timestamp DESC
      LIMIT  1
  )
WHERE cp.is_active = 1
  AND er.rate_timestamp < DATE_SUB(NOW(), INTERVAL ${HOURS} HOUR)
ORDER BY er.rate_timestamp ASC;

EOF2

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Report completed"
else
    echo "✗ Error: Report failed"
    exit 1
fi