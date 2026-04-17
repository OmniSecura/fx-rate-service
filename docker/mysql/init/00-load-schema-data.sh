#!/bin/bash
set -euo pipefail

echo "Loading schema into ${MYSQL_DATABASE}..."

TABLE_SCRIPTS=(
  "/seed/sql/tables/currency.sql"
  "/seed/sql/tables/rate_provider.sql"
  "/seed/sql/tables/currency_pair.sql"
  "/seed/sql/tables/exchange_rate.sql"
  "/seed/sql/tables/eod_fixing.sql"
  "/seed/sql/tables/forward_rate.sql"
  "/seed/sql/tables/rate_alert.sql"
  "/seed/sql/tables/rate_audit_log.sql"
)

for file in "${TABLE_SCRIPTS[@]}"; do
  mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "${file}"
done

echo "Loading seed data..."
DATA_SCRIPTS=(
  "/seed/data/01_data_currencies.sql"
  "/seed/data/02_data_rate_providers.sql"
  "/seed/data/03_data_currency_pairs.sql"
  "/seed/data/04_data_exchange_rates.sql"
  "/seed/data/05_data_eod_fixings.sql"
  "/seed/data/06_data_forward_rates.sql"
  "/seed/data/07_data_rate_alerts.sql"
  "/seed/data/08_data_rate_audit_log.sql"
)

for file in "${DATA_SCRIPTS[@]}"; do
  mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "${file}"
done

echo "Loading views..."
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/views/latest_rates_vw.sql"
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/views/rate_history_vw.sql"

echo "Loading stored procedures..."
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/stored_procedures/store_rate.sql"
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/stored_procedures/store_fixing.sql"
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/stored_procedures/get_rate.sql"
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" < "/seed/sql/stored_procedures/get_cross_rate.sql"

echo "MySQL bootstrap complete."

