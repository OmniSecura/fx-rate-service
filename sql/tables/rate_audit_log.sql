-- ============================================================
--  FX Rate Service — DDL
--  Table: rate_audit_log
-- ============================================================

DROP TABLE IF EXISTS rate_audit_log;

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

    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id),

    CONSTRAINT chk_action CHECK (action IN ('INSERT', 'UPDATE', 'INVALIDATE'))
);
