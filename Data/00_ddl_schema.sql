-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — DDL Schema
--
--  Compatible with PostgreSQL and MySQL 8+.
--  Run this file first, then load data files 01–08 in order.
-- ============================================================

DROP TABLE IF EXISTS rate_audit_log;
DROP TABLE IF EXISTS rate_alert;
DROP TABLE IF EXISTS forward_rate;
DROP TABLE IF EXISTS eod_fixing;
DROP TABLE IF EXISTS exchange_rate;
DROP TABLE IF EXISTS currency_pair;
DROP TABLE IF EXISTS rate_provider;
DROP TABLE IF EXISTS currency;

-- ── currency ──────────────────────────────────────────────
CREATE TABLE currency (
    currency_id     INT             PRIMARY KEY,
    iso_code        CHAR(3)         NOT NULL UNIQUE,   -- ISO 4217
    currency_name   VARCHAR(60)     NOT NULL,
    country         VARCHAR(60)     NOT NULL,
    numeric_code    CHAR(3)         NOT NULL UNIQUE,
    minor_units     SMALLINT        NOT NULL DEFAULT 2, -- decimal places
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    region          VARCHAR(20)     NOT NULL            -- EMEA / AMER / APAC
);

-- ── rate_provider ─────────────────────────────────────────
CREATE TABLE rate_provider (
    provider_id     INT             PRIMARY KEY,
    provider_code   VARCHAR(15)     NOT NULL UNIQUE,
    provider_name   VARCHAR(80)     NOT NULL,
    provider_type   VARCHAR(20)     NOT NULL,   -- MARKET_DATA / CENTRAL_BANK / INTERNAL / BROKER
    country         CHAR(2)         NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    priority        SMALLINT        NOT NULL DEFAULT 1  -- 1 = highest priority source
);

-- ── currency_pair ─────────────────────────────────────────
CREATE TABLE currency_pair (
    pair_id         INT             PRIMARY KEY,
    pair_code       VARCHAR(7)      NOT NULL UNIQUE,    -- e.g. EUR/USD
    base_currency   CHAR(3)         NOT NULL,
    quote_currency  CHAR(3)         NOT NULL,
    pair_type       VARCHAR(15)     NOT NULL,   -- MAJOR / MINOR / EXOTIC / CROSS
    decimal_places  SMALLINT        NOT NULL DEFAULT 4,
    pip_size        DECIMAL(10,6)   NOT NULL DEFAULT 0.0001,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (base_currency)  REFERENCES currency(iso_code),
    FOREIGN KEY (quote_currency) REFERENCES currency(iso_code)
);

-- ── exchange_rate ──────────────────────────────────────────
CREATE TABLE exchange_rate (
    rate_id         INT             PRIMARY KEY,
    pair_id         INT             NOT NULL,
    provider_id     INT             NOT NULL,
    bid_rate        DECIMAL(18,6)   NOT NULL,
    ask_rate        DECIMAL(18,6)   NOT NULL,
    mid_rate        DECIMAL(18,6)   NOT NULL,
    rate_timestamp  TIMESTAMP       NOT NULL,
    source_system   VARCHAR(30)     NOT NULL,   -- REUTERS / BLOOMBERG / ECB_FEED / INTERNAL
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (pair_id)     REFERENCES currency_pair(pair_id),
    FOREIGN KEY (provider_id) REFERENCES rate_provider(provider_id)
);

CREATE INDEX idx_exrate_pair_ts ON exchange_rate(pair_id, rate_timestamp);
CREATE INDEX idx_exrate_provider ON exchange_rate(provider_id);

-- ── eod_fixing ────────────────────────────────────────────
CREATE TABLE eod_fixing (
    fixing_id       INT             PRIMARY KEY,
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
    UNIQUE (pair_id, fixing_date, fixing_type)
);

CREATE INDEX idx_fixing_date ON eod_fixing(fixing_date);

-- ── forward_rate ──────────────────────────────────────────
CREATE TABLE forward_rate (
    forward_id      INT             PRIMARY KEY,
    pair_id         INT             NOT NULL,
    provider_id     INT             NOT NULL,
    tenor           VARCHAR(5)      NOT NULL,   -- ON / TN / 1W / 1M / 2M / 3M / 6M / 1Y
    value_date      DATE            NOT NULL,
    forward_points  DECIMAL(10,4)   NOT NULL,   -- pips added to spot
    forward_rate    DECIMAL(18,6)   NOT NULL,
    rate_timestamp  TIMESTAMP       NOT NULL,
    FOREIGN KEY (pair_id)     REFERENCES currency_pair(pair_id),
    FOREIGN KEY (provider_id) REFERENCES rate_provider(provider_id)
);

-- ── rate_alert ────────────────────────────────────────────
CREATE TABLE rate_alert (
    alert_id        INT             PRIMARY KEY,
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
    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id)
);

-- ── rate_audit_log ────────────────────────────────────────
CREATE TABLE rate_audit_log (
    log_id          INT             PRIMARY KEY,
    rate_id         INT             NOT NULL,
    pair_id         INT             NOT NULL,
    action          VARCHAR(10)     NOT NULL,   -- INSERT / UPDATE / INVALIDATE
    old_mid_rate    DECIMAL(18,6),
    new_mid_rate    DECIMAL(18,6),
    change_pct      DECIMAL(8,4),               -- % change
    changed_by      VARCHAR(50)     NOT NULL,
    changed_at      TIMESTAMP       NOT NULL,
    reason          VARCHAR(120),
    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id)
);
