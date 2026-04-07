-- latest_rates_vw

CREATE OR REPLACE VIEW latest_rates_vw AS
SELECT
    cp.pair_code,
    cp.pair_type,
    er.mid_rate,
    er.rate_timestamp,
    cp.is_active,
    TIMESTAMPDIFF(HOUR, er.rate_timestamp, NOW()) AS hours_since_update,
    CASE
        WHEN TIMESTAMPDIFF(HOUR, er.rate_timestamp, NOW()) > 4 THEN 1
        ELSE 0
    END AS is_stale
FROM currency_pair cp
JOIN exchange_rate er
  ON er.rate_id = (
      SELECT rate_id
      FROM exchange_rate
      WHERE pair_id = cp.pair_id
        AND is_valid = 1
      ORDER BY rate_timestamp DESC
      LIMIT 1
  )
WHERE cp.is_active = 1;