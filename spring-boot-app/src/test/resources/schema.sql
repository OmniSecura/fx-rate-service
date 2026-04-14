-- ============================================================
--  FX Rate Service — DDL
--  Table: currency
-- ============================================================

DROP TABLE IF EXISTS currency;

CREATE TABLE currency (
    currency_id     INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    iso_code        CHAR(3)         NOT NULL UNIQUE,   -- ISO 4217
    currency_name   VARCHAR(60)     NOT NULL,
    country         VARCHAR(60)     NOT NULL,
    numeric_code    CHAR(3)         NOT NULL UNIQUE,
    minor_units     SMALLINT        NOT NULL DEFAULT 2, -- decimal places
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    region          VARCHAR(20)     NOT NULL,           -- EMEA / AMER / APAC

    CONSTRAINT chk_minor_units  CHECK (minor_units >= 0 AND minor_units <= 4),
    CONSTRAINT chk_region       CHECK (region IN ('EMEA', 'AMER', 'APAC'))
);

-- ============================================================
--  FX Rate Service — DDL
--  Table: rate_provider
-- ============================================================

DROP TABLE IF EXISTS rate_provider;

CREATE TABLE rate_provider (
    provider_id     INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_code   VARCHAR(15)     NOT NULL UNIQUE,
    provider_name   VARCHAR(80)     NOT NULL,
    provider_type   VARCHAR(20)     NOT NULL,   -- MARKET_DATA / CENTRAL_BANK / INTERNAL / BROKER
    country         CHAR(2)         NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    priority        SMALLINT        NOT NULL DEFAULT 1,  -- 1 = highest priority source

    CONSTRAINT chk_provider_type CHECK (provider_type IN ('MARKET_DATA', 'CENTRAL_BANK', 'INTERNAL', 'BROKER')),
    CONSTRAINT chk_priority      CHECK (priority >= 1)
);

-- ============================================================
--  FX Rate Service — DDL
--  Table: currency_pair
-- ============================================================

DROP TABLE IF EXISTS currency_pair;

CREATE TABLE currency_pair (
    pair_id         INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pair_code       VARCHAR(7)      NOT NULL UNIQUE,    -- e.g. EUR/USD
    base_currency   CHAR(3)         NOT NULL,
    quote_currency  CHAR(3)         NOT NULL,
    pair_type       VARCHAR(15)     NOT NULL,   -- MAJOR / MINOR / EXOTIC / CROSS
    decimal_places  SMALLINT        NOT NULL DEFAULT 4,
    pip_size        DECIMAL(10,6)   NOT NULL DEFAULT 0.0001,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    FOREIGN KEY (base_currency)  REFERENCES currency(iso_code),
    FOREIGN KEY (quote_currency) REFERENCES currency(iso_code),

    CONSTRAINT chk_pip_size             CHECK (pip_size > 0),
    CONSTRAINT chk_decimal_places       CHECK (decimal_places >= 0 AND decimal_places <= 6),
    CONSTRAINT chk_different_currencies CHECK (base_currency <> quote_currency),
    CONSTRAINT chk_pair_type            CHECK (pair_type IN ('MAJOR', 'MINOR', 'EXOTIC', 'CROSS'))
);

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

-- ============================================================
--  FX Rate Service — DDL
--  Table: eod_fixing
-- ============================================================

DROP TABLE IF EXISTS eod_fixing;

CREATE TABLE eod_fixing (
    fixing_id       INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pair_id         INT             NOT NULL,
    provider_id     INT             NOT NULL,
    fixing_date     DATE            NOT NULL,
    fixing_rate     DECIMAL(18,6)   NOT NULL,
    fixing_time     VARCHAR(10)     NOT NULL,   -- e.g. '16:00 LON', '11:00 ECB'
    fixing_type     VARCHAR(20)     NOT NULL,   -- WMR / ECB / BFIX / INTERNAL
    is_official     BOOLEAN         NOT NULL DEFAULT TRUE,
    published_at    TIMESTAMP       NOT NULL,

    FOREIGN KEY (pair_id)     REFERENCES currency_pair(pair_id),
    FOREIGN KEY (provider_id) REFERENCES rate_provider(provider_id),

    UNIQUE (pair_id, fixing_date, fixing_type),

    CONSTRAINT chk_fixing_rate_positive CHECK (fixing_rate > 0),
    CONSTRAINT chk_fixing_type          CHECK (fixing_type IN ('WMR', 'ECB', 'BFIX', 'INTERNAL')),
    CONSTRAINT chk_published_after_date CHECK (published_at >= fixing_date)
);

CREATE INDEX idx_fixing_date ON eod_fixing(fixing_date);

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

-- ============================================================
--  FX Rate Service — DDL
--  Table: rate_alert
-- ============================================================

DROP TABLE IF EXISTS rate_alert;

CREATE TABLE rate_alert (
    alert_id        INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pair_id         INT             NOT NULL,
    alert_type      VARCHAR(20)     NOT NULL,   -- THRESHOLD_BREACH / STALE_RATE / SPREAD_WIDE / SPIKE
    threshold_value DECIMAL(18,6),
    actual_value    DECIMAL(18,6),
    alert_message   VARCHAR(255)    NOT NULL,
    severity        VARCHAR(10)     NOT NULL,   -- INFO / WARNING / CRITICAL
    triggered_at    TIMESTAMP       NOT NULL,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(50),
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN',  -- OPEN / ACKNOWLEDGED / RESOLVED

    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id),

    CONSTRAINT chk_severity   CHECK (severity   IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_status     CHECK (status     IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('THRESHOLD_BREACH', 'STALE_RATE', 'SPREAD_WIDE', 'SPIKE')),
    CONSTRAINT chk_ack_after_trigger CHECK (acknowledged_at IS NULL OR acknowledged_at >= triggered_at)
);

-- ============================================================
--  FX Rate Service — DDL
--  Table: rate_audit_log
-- ============================================================

DROP TABLE IF EXISTS rate_audit_log;

CREATE TABLE rate_audit_log (
    log_id          INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rate_id         INT             NOT NULL,
    pair_id         INT             NOT NULL,
    action          VARCHAR(10)     NOT NULL,   -- INSERT / UPDATE / INVALIDATE
    old_mid_rate    DECIMAL(18,6),
    new_mid_rate    DECIMAL(18,6),
    change_pct      DECIMAL(8,4),               -- % change
    changed_by      VARCHAR(50)     NOT NULL,
    changed_at      TIMESTAMP       NOT NULL,
    reason          VARCHAR(120),

    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id),

    CONSTRAINT chk_action CHECK (action IN ('INSERT', 'UPDATE', 'INVALIDATE'))
);
