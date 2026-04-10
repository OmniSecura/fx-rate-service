#!/bin/bash

MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql"

HOURS=${1:-4}

echo "➜ Stale rates report"
echo "  Threshold: ${HOURS} hours"
echo ""

"$MYSQL" -u root -p fx_rate_db <<EOF

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
ORDER BY rate_timestamp ASC;

EOF

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Report completed"
else
    echo "✗ Error: Report failed"
    exit 1
fi