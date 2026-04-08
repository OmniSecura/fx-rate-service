-- ============================================================
--  Stored Procedure: latest_rates_vw
--  Returns the most recent valid spot rate for every active
--  currency pair, with a staleness flag if the rate has not
--  been updated in over 4 hours.
--
--  Example:
--    SELECT * FROM latest_rates_vw;
--    SELECT * FROM latest_rates_vw WHERE is_stale = 1;
-- ============================================================

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

-- ============================================================
--  View: rate_history_vw
--  Full rate history for all pairs joined with pair and
--  provider details, ordered newest first.
--  Use for audit queries and historical analysis.
--
--  Example:
--    SELECT * FROM rate_history_vw WHERE pair_code = 'GBP/USD';
--    SELECT * FROM rate_history_vw WHERE pair_code = 'EUR/USD'
--      AND DATE(rate_timestamp) = '2026-03-26';
-- ============================================================

-- rate_history_vw

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

-- ============================================================
--  Stored Procedure: store_rate
--  Inserts a new spot rate for a currency pair and marks all
--  previous valid rates for that pair as stale.
--  Calculates mid rate automatically from bid and ask.
--  Raises an error if bid > ask.
--
--  Example:
--    CALL store_rate(21, 1, 1, 1.08290, 1.08310, NOW(), 'REUTERS');
--
--  Arguments:
--    p_rate_id        primary key for the new rate
--    p_pair_id        currency pair ID
--    p_provider_id    rate provider ID
--    p_bid_rate       bid rate
--    p_ask_rate       ask rate
--    p_rate_timestamp when the rate was captured
--    p_source_system  e.g. REUTERS, BLOOMBERG
-- ============================================================

-- store_rate

ALTER TABLE exchange_rate
ADD COLUMN is_stale BOOLEAN NOT NULL DEFAULT FALSE;

DELIMITER $$

CREATE PROCEDURE store_rate (
    IN p_rate_id INT,
    IN p_pair_id INT,
    IN p_provider_id INT,
    IN p_bid_rate DECIMAL(18,6),
    IN p_ask_rate DECIMAL(18,6),
    IN p_rate_timestamp TIMESTAMP,
    IN p_source_system VARCHAR(30)
)
BEGIN
    DECLARE v_mid_rate DECIMAL(18,6);

    IF p_bid_rate > p_ask_rate THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid rate: bid_rate cannot be greater than ask_rate';
    END IF;

    SET v_mid_rate = ROUND((p_bid_rate + p_ask_rate) / 2, 6);

    UPDATE exchange_rate
    SET is_stale = TRUE
    WHERE pair_id = p_pair_id
      AND is_valid = TRUE
      AND is_stale = FALSE;

    INSERT INTO exchange_rate (
        rate_id,
        pair_id,
        provider_id,
        bid_rate,
        ask_rate,
        mid_rate,
        rate_timestamp,
        source_system,
        is_valid,
        is_stale
    )
    VALUES (
        p_rate_id,
        p_pair_id,
        p_provider_id,
        p_bid_rate,
        p_ask_rate,
        v_mid_rate,
        p_rate_timestamp,
        p_source_system,
        TRUE,
        FALSE
    );
END $$

DELIMITER ;

-- ============================================================
--  Stored Procedure: store_fixing
--  Inserts an end-of-day fixing into eod_fixing.
--  If the fixing rate deviates from the last traded mid rate
--  by more than the threshold (default 1%), a WARNING alert
--  is automatically inserted into rate_alert.
--
--  Example:
--    CALL store_fixing(21, 1, 8, '2026-04-08', 1.0756, '16:00 LON', 'WMR', TRUE, NOW(), NULL);
--    CALL store_fixing(21, 1, 8, '2026-04-08', 1.0756, '16:00 LON', 'WMR', TRUE, NOW(), 0.005);
--
--  Arguments:
--    p_fixing_id      primary key for the fixing
--    p_pair_id        currency pair ID
--    p_provider_id    rate provider ID
--    p_fixing_date    date of the fixing
--    p_fixing_rate    official fixing rate
--    p_fixing_time    e.g. '16:00 LON', '11:00 ECB'
--    p_fixing_type    WMR / ECB / BFIX / INTERNAL
--    p_is_official    whether this is an official benchmark fixing
--    p_published_at   when the fixing was published
--    p_threshold      deviation threshold e.g. 0.01 = 1% (NULL = default 1%)
-- ============================================================

-- store_fixing

DELIMITER $$
 
CREATE PROCEDURE store_fixing (
    IN p_fixing_id    INT,
    IN p_pair_id      INT,
    IN p_provider_id  INT,
    IN p_fixing_date  DATE,
    IN p_fixing_rate  DECIMAL(18,6),
    IN p_fixing_time  VARCHAR(10),
    IN p_fixing_type  VARCHAR(20),
    IN p_is_official  BOOLEAN,
    IN p_published_at TIMESTAMP,
    IN p_threshold    DECIMAL(5,4)
)
BEGIN
    DECLARE v_last_mid_rate  DECIMAL(18,6);
    DECLARE v_deviation_pct  DECIMAL(8,4);
    DECLARE v_threshold      DECIMAL(5,4);
 
    SET v_threshold = IFNULL(p_threshold, 0.01);
 
    SELECT mid_rate INTO v_last_mid_rate
    FROM exchange_rate
    WHERE pair_id = p_pair_id
      AND is_valid = 1
    ORDER BY rate_timestamp DESC
    LIMIT 1;
 
    IF v_last_mid_rate IS NOT NULL AND v_last_mid_rate != 0 THEN
        SET v_deviation_pct = ABS((p_fixing_rate - v_last_mid_rate) / v_last_mid_rate);
 
        IF v_deviation_pct > v_threshold THEN
            INSERT INTO rate_alert (
                alert_id,
                pair_id,
                alert_type,
                threshold_value,
                actual_value,
                alert_message,
                severity,
                triggered_at,
                status
            )
            VALUES (
                (SELECT IFNULL(MAX(alert_id), 0) + 1 FROM rate_alert ra),
                p_pair_id,
                'THRESHOLD_BREACH',
                v_threshold,
                v_deviation_pct,
                CONCAT('Fixing deviation alert: fixing=', p_fixing_rate,
                       ' lastTraded=', v_last_mid_rate,
                       ' deviation=', ROUND(v_deviation_pct * 100, 2), '%'),
                'WARNING',
                NOW(),
                'OPEN'
            );
        END IF;
    END IF;
 
    INSERT INTO eod_fixing (
        fixing_id,
        pair_id,
        provider_id,
        fixing_date,
        fixing_rate,
        fixing_time,
        fixing_type,
        is_official,
        published_at
    )
    VALUES (
        p_fixing_id,
        p_pair_id,
        p_provider_id,
        p_fixing_date,
        p_fixing_rate,
        p_fixing_time,
        p_fixing_type,
        p_is_official,
        p_published_at
    );
 
END $$
 
DELIMITER ;

-- ============================================================
--  Stored Procedure: get_rate
--  Returns the latest valid spot rate for a pair with staleness flag.
--
--  Example:
--    CALL get_rate('EUR/USD', 60);
--
--  Arguments:
--    p_pair_code      currency pair code, e.g. 'EUR/USD'
--    p_stale_minutes  staleness threshold in minutes (e.g. 60)
-- ============================================================

-- get_rate

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

-- ============================================================
--  Stored Procedure: get_cross_rate
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

-- get_cross_rate 

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
        SELECT NULL AS cross_pair, NULL AS cross_rate, NULL AS mid1, NULL AS mid2, NULL AS is_stale1, NULL AS is_stale2, NULL AS rate1_time, NULL AS rate2_time;
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