-- ============================================================
--  FX Rate Service — Stored Procedure
--  Procedure: get_cross_rate
--
--  Calculates implied cross rate from two overlapping pairs.
--
--  Example:
--    CALL get_cross_rate('EUR/USD', 'USD/PLN', 60);
--
--  Arguments:
--    p_pair1         first currency pair code, e.g. 'EUR/USD'
--    p_pair2         second currency pair code, e.g. 'USD/PLN'
--    p_stale_minutes staleness threshold in minutes (e.g. 60)
-- ============================================================

DROP PROCEDURE IF EXISTS get_cross_rate;

DELIMITER $$

CREATE PROCEDURE get_cross_rate(
    IN p_pair1 VARCHAR(7),
    IN p_pair2 VARCHAR(7),
    IN p_stale_minutes INT
)
main_block: BEGIN
    DECLARE v_stale_minutes INT DEFAULT 60;
    DECLARE v_base1 VARCHAR(3);
    DECLARE v_quote1 VARCHAR(3);
    DECLARE v_base2 VARCHAR(3);
    DECLARE v_quote2 VARCHAR(3);
    DECLARE v_common VARCHAR(3);
    DECLARE v_cross_base VARCHAR(3);
    DECLARE v_cross_quote VARCHAR(3);
    DECLARE v_mid1 DECIMAL(18,6);
    DECLARE v_mid2 DECIMAL(18,6);
    DECLARE v_rate1_time TIMESTAMP;
    DECLARE v_rate2_time TIMESTAMP;
    DECLARE v_is_stale1 BOOLEAN;
    DECLARE v_is_stale2 BOOLEAN;
    DECLARE v_cross_rate DECIMAL(18,6);

    IF p_stale_minutes IS NOT NULL AND p_stale_minutes > 0 THEN
        SET v_stale_minutes = p_stale_minutes;
    END IF;

    SET v_base1 = SUBSTRING_INDEX(p_pair1, '/', 1);
    SET v_quote1 = SUBSTRING_INDEX(p_pair1, '/', -1);
    SET v_base2 = SUBSTRING_INDEX(p_pair2, '/', 1);
    SET v_quote2 = SUBSTRING_INDEX(p_pair2, '/', -1);

    IF v_base1 = v_base2 THEN
        SET v_common = v_base1;
        SET v_cross_base = v_quote1;
        SET v_cross_quote = v_quote2;
    ELSEIF v_base1 = v_quote2 THEN
        SET v_common = v_base1;
        SET v_cross_base = v_quote1;
        SET v_cross_quote = v_base2;
    ELSEIF v_quote1 = v_base2 THEN
        SET v_common = v_quote1;
        SET v_cross_base = v_base1;
        SET v_cross_quote = v_quote2;
    ELSEIF v_quote1 = v_quote2 THEN
        SET v_common = v_quote1;
        SET v_cross_base = v_base1;
        SET v_cross_quote = v_base2;
    ELSE
        SELECT NULL AS cross_pair, NULL AS cross_rate, NULL AS mid1, NULL AS mid2,
               NULL AS is_stale1, NULL AS is_stale2, NULL AS rate1_time, NULL AS rate2_time;
        LEAVE main_block;
    END IF;

    SELECT er.mid_rate, er.rate_timestamp,
           (er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE)) AS is_stale
      INTO v_mid1, v_rate1_time, v_is_stale1
      FROM currency_pair cp
      LEFT JOIN exchange_rate er ON er.rate_id = (
        SELECT er2.rate_id FROM exchange_rate er2
         WHERE er2.pair_id = cp.pair_id AND er2.is_valid = TRUE
         ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC LIMIT 1)
     WHERE cp.pair_code = p_pair1 AND cp.is_active = TRUE;

    SELECT er.mid_rate, er.rate_timestamp,
           (er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE)) AS is_stale
      INTO v_mid2, v_rate2_time, v_is_stale2
      FROM currency_pair cp
      LEFT JOIN exchange_rate er ON er.rate_id = (
        SELECT er2.rate_id FROM exchange_rate er2
         WHERE er2.pair_id = cp.pair_id AND er2.is_valid = TRUE
         ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC LIMIT 1)
     WHERE cp.pair_code = p_pair2 AND cp.is_active = TRUE;

    IF v_mid1 IS NULL OR v_mid2 IS NULL THEN
        SET v_cross_rate = NULL;
    ELSEIF v_common = v_quote1 AND v_common = v_base2 THEN
        SET v_cross_rate = v_mid1 * v_mid2;
    ELSEIF v_common = v_base1 AND v_common = v_quote2 THEN
        SET v_cross_rate = v_mid2 * v_mid1;
    ELSEIF v_common = v_quote1 AND v_common = v_quote2 THEN
        SET v_cross_rate = v_mid1 / v_mid2;
    ELSEIF v_common = v_base1 AND v_common = v_base2 THEN
        SET v_cross_rate = v_mid2 / v_mid1;
    ELSE
        SET v_cross_rate = NULL;
    END IF;

    SELECT CONCAT(v_cross_base, '/', v_cross_quote) AS cross_pair,
           v_cross_rate AS cross_rate,
           v_mid1 AS mid1,
           v_mid2 AS mid2,
           v_is_stale1 AS is_stale1,
           v_is_stale2 AS is_stale2,
           v_rate1_time AS rate1_time,
           v_rate2_time AS rate2_time;
END$$

DELIMITER ;
