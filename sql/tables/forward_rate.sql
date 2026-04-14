-- ============================================================
--  FX Rate Service — DDL
--  Table: forward_rate
-- ============================================================

DROP TABLE IF EXISTS forward_rate;

CREATE TABLE forward_rate (
    forward_id      INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pair_id         INT             NOT NULL,
    provider_id     INT             NOT NULL,
    tenor           VARCHAR(5)      NOT NULL,   -- ON / TN / 1W / 1M / 2M / 3M / 6M / 1Y
    value_date      DATE            NOT NULL,
    forward_points  DECIMAL(10,4)   NOT NULL,   -- pips added to spot (can be negative)
    forward_rate    DECIMAL(18,6)   NOT NULL,
    rate_timestamp  TIMESTAMP       NOT NULL,

    FOREIGN KEY (pair_id)     REFERENCES currency_pair(pair_id),
    FOREIGN KEY (provider_id) REFERENCES rate_provider(provider_id),

    CONSTRAINT chk_forward_rate_positive CHECK (forward_rate > 0),
    CONSTRAINT chk_tenor                 CHECK (tenor IN ('ON', 'TN', '1W', '1M', '2M', '3M', '6M', '1Y')),
    CONSTRAINT chk_value_date_future     CHECK (value_date >= rate_timestamp)
);
