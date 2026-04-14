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
