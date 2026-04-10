-- ============================================================
--  FX Rate Service — DDL
--  Table: rate_alert
-- ============================================================

DROP TABLE IF EXISTS rate_alert;

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

    FOREIGN KEY (pair_id) REFERENCES currency_pair(pair_id),

    CONSTRAINT chk_severity   CHECK (severity   IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_status     CHECK (status     IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('THRESHOLD_BREACH', 'STALE_RATE', 'SPREAD_WIDE', 'SPIKE')),
    CONSTRAINT chk_ack_after_trigger CHECK (acknowledged_at IS NULL OR acknowledged_at >= triggered_at)
);
