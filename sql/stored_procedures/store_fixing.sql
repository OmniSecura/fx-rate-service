-- ============================================================
--  FX Rate Service — Stored Procedure
--  Procedure: store_fixing
--
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
