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
