-- ============================================================
--  FX Rate Service — DDL
--  Table: exchange_rate
-- ============================================================

DROP TABLE IF EXISTS exchange_rate;

CREATE TABLE exchange_rate (
    rate_id         INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pair_id         INT             NOT NULL,
    provider_id     INT             NOT NULL,
    bid_rate        DECIMAL(18,6)   NOT NULL,
    ask_rate        DECIMAL(18,6)   NOT NULL,
    mid_rate        DECIMAL(18,6)   NOT NULL,
    rate_timestamp  TIMESTAMP       NOT NULL,
    source_system   VARCHAR(30)     NOT NULL,   -- REUTERS / BLOOMBERG / ECB_FEED / INTERNAL
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    FOREIGN KEY (pair_id)     REFERENCES currency_pair(pair_id),
    FOREIGN KEY (provider_id) REFERENCES rate_provider(provider_id),

    CONSTRAINT chk_spread        CHECK (bid_rate <= mid_rate AND mid_rate <= ask_rate),
    CONSTRAINT chk_rates_positive CHECK (bid_rate > 0 AND ask_rate > 0 AND mid_rate > 0),
    CONSTRAINT chk_source_system CHECK (source_system IN ('REUTERS', 'BLOOMBERG', 'ECB_FEED', 'HSBC_INT', 'WMR', 'ICAP'))
);

CREATE INDEX idx_exrate_pair_ts ON exchange_rate(pair_id, rate_timestamp);
CREATE INDEX idx_exrate_provider ON exchange_rate(provider_id);
