-- views 

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

-- rate_history_vm

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

-- procedures

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

