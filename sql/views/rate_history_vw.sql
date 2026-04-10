-- ============================================================
--  FX Rate Service — View
--  View: rate_history_vw
--
--  Full rate history for all pairs joined with pair and
--  provider details, ordered newest first.
--  Use for audit queries and historical analysis.
--
--  Example:
--    SELECT * FROM rate_history_vw WHERE pair_code = 'GBP/USD';
--    SELECT * FROM rate_history_vw WHERE pair_code = 'EUR/USD'
--      AND DATE(rate_timestamp) = '2026-03-26';
-- ============================================================

CREATE OR REPLACE VIEW rate_history_vw AS
SELECT
    er.rate_id,
    er.pair_id,
    cp.pair_code,
    cp.base_currency,
    cp.quote_currency,
    cp.pair_type,
    er.provider_id,
    rp.provider_code,
    rp.provider_name,
    er.bid_rate,
    er.ask_rate,
    er.mid_rate,
    ROUND(er.ask_rate - er.bid_rate, 6) AS spread,
    er.rate_timestamp,
    er.source_system,
    er.is_valid
FROM exchange_rate er
JOIN currency_pair cp
    ON er.pair_id = cp.pair_id
JOIN rate_provider rp
    ON er.provider_id = rp.provider_id
ORDER BY er.rate_timestamp DESC;
