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
