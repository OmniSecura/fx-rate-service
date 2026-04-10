-- ============================================================
--  FX Rate Service — Stored Procedure
--  Procedure: get_rate
--
--  Returns the latest valid spot rate for a pair with staleness flag.
--
--  Example:
--    CALL get_rate('EUR/USD', 60);
--
--  Arguments:
--    p_pair_code      currency pair code, e.g. 'EUR/USD'
--    p_stale_minutes  staleness threshold in minutes (e.g. 60)
-- ============================================================

DROP PROCEDURE IF EXISTS get_rate;

DELIMITER $$

CREATE PROCEDURE get_rate(
    IN p_pair_code VARCHAR(7),
    IN p_stale_minutes INT
)
BEGIN
    DECLARE v_stale_minutes INT DEFAULT 60;

    IF p_stale_minutes IS NOT NULL AND p_stale_minutes > 0 THEN
        SET v_stale_minutes = p_stale_minutes;
    END IF;

    SELECT
        cp.pair_code,
        er.mid_rate,
        er.rate_timestamp,
        CASE
            WHEN er.rate_timestamp IS NULL THEN NULL
            WHEN er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE) THEN TRUE
            ELSE FALSE
        END AS is_stale,
        TIMESTAMPDIFF(MINUTE, er.rate_timestamp, UTC_TIMESTAMP()) AS age_minutes
    FROM currency_pair cp
    LEFT JOIN exchange_rate er
        ON er.rate_id = (
            SELECT er2.rate_id
            FROM exchange_rate er2
            WHERE er2.pair_id = cp.pair_id
              AND er2.is_valid = TRUE
            ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC
            LIMIT 1
        )
    WHERE cp.pair_code = p_pair_code
      AND cp.is_active = TRUE;
END$$

DELIMITER ;
