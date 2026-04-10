-- ============================================================
--  FX Rate Service — Stored Procedure
--  Procedure: store_rate
--
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

-- Prerequisite: add is_stale column if not already present
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
