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
