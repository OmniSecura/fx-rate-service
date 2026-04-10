-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Sample Data
--  Table : rate_alert
--  Rows  : 20
--
--  Compatible with: PostgreSQL, MySQL 8+
--  Oracle note    : Replace TIMESTAMP literals with
--                   TO_TIMESTAMP equivalents
--
--  Load order:
--    01_currencies → 02_rate_providers → 03_currency_pairs →
--    04_exchange_rates → 05_eod_fixings → 06_forward_rates →
--    07_rate_alerts → 08_rate_audit_log
-- ============================================================

INSERT INTO rate_alert
    (alert_id, pair_id, alert_type, threshold_value, actual_value, alert_message, severity, triggered_at, acknowledged_at, acknowledged_by, status)
VALUES
    (1, 1, 'THRESHOLD_BREACH', 1.09, 1.0831, 'EUR/USD dropped below 1.0900 threshold', 'WARNING', '2026-03-26 07:45:12', '2026-03-26 08:02:33', 'fx.desk@hsbc.com', 'RESOLVED'),
    (2, 3, 'SPIKE', NULL, 150.21, 'USD/JPY moved 80 pips in 60 seconds', 'CRITICAL', '2026-03-26 04:12:08', '2026-03-26 04:18:44', 'asia.desk@hsbc.com', 'RESOLVED'),
    (3, 17, 'THRESHOLD_BREACH', 84.0, 83.626, 'USD/INR below 84.000 alert level', 'INFO', '2026-03-26 05:30:00', NULL, NULL, 'OPEN'),
    (4, 20, 'SPREAD_WIDE', 0.01, 0.0026, 'USD/ZAR spread 26 pips — exceeds 10 pip SLA', 'WARNING', '2026-03-26 06:00:14', '2026-03-26 06:15:02', 'ops@hsbc.com', 'RESOLVED'),
    (5, 2, 'STALE_RATE', NULL, NULL, 'GBP/USD rate not updated for 45 seconds', 'WARNING', '2026-03-25 16:59:55', '2026-03-25 17:00:10', 'fx.desk@hsbc.com', 'RESOLVED'),
    (6, 19, 'SPIKE', NULL, 20.65, 'USD/MXN spiked to 20.65 — possible illiquid fill', 'CRITICAL', '2026-03-25 14:22:31', '2026-03-25 14:25:00', 'latam.desk@hsbc.com', 'RESOLVED'),
    (7, 16, 'THRESHOLD_BREACH', 7.3, 7.234, 'USD/CNH moved above 7.300 watch level', 'INFO', '2026-03-24 09:11:42', NULL, NULL, 'OPEN'),
    (8, 4, 'SPREAD_WIDE', 0.0012, 0.0006, 'USD/CHF spread within SLA — auto-resolved', 'INFO', '2026-03-24 10:00:00', '2026-03-24 10:00:01', 'system', 'RESOLVED'),
    (9, 9, 'THRESHOLD_BREACH', 163.0, 162.25, 'EUR/JPY below 163.00 — options barrier watch', 'WARNING', '2026-03-24 11:30:18', '2026-03-24 11:45:00', 'fx.desk@hsbc.com', 'ACKNOWLEDGED'),
    (10, 5, 'STALE_RATE', NULL, NULL, 'AUD/USD rate stale during APAC liquidity gap', 'WARNING', '2026-03-24 03:15:44', '2026-03-24 03:20:00', 'apac.desk@hsbc.com', 'RESOLVED'),
    (11, 1, 'THRESHOLD_BREACH', 1.1, 1.0831, 'EUR/USD still below 1.1000 long-term level', 'INFO', '2026-03-23 09:00:00', NULL, NULL, 'OPEN'),
    (12, 15, 'SPIKE', NULL, 1.3621, 'USD/SGD spike detected in Asian open', 'WARNING', '2026-03-23 01:30:22', '2026-03-23 01:38:00', 'apac.desk@hsbc.com', 'RESOLVED'),
    (13, 20, 'THRESHOLD_BREACH', 19.0, 18.883, 'USD/ZAR dropped below 19.000 — EM risk-on', 'INFO', '2026-03-22 09:45:00', NULL, NULL, 'OPEN'),
    (14, 3, 'THRESHOLD_BREACH', 152.0, 149.87, 'USD/JPY below 152 — BOJ intervention watch', 'CRITICAL', '2026-03-21 08:00:00', '2026-03-21 08:30:00', 'rates.desk@hsbc.com', 'RESOLVED'),
    (15, 6, 'SPREAD_WIDE', 0.0015, 0.0006, 'USD/CAD spread normalised after BoC meeting', 'INFO', '2026-03-20 15:00:00', '2026-03-20 15:01:00', 'system', 'RESOLVED'),
    (16, 2, 'THRESHOLD_BREACH', 1.3, 1.296, 'GBP/USD below 1.3000 — BoE policy watch', 'WARNING', '2026-03-20 09:30:00', '2026-03-20 10:00:00', 'fx.desk@hsbc.com', 'RESOLVED'),
    (17, 11, 'STALE_RATE', NULL, NULL, 'EUR/CHF stale — SNB rate decision pending', 'INFO', '2026-03-19 08:29:50', '2026-03-19 08:35:00', 'emea.desk@hsbc.com', 'RESOLVED'),
    (18, 18, 'THRESHOLD_BREACH', 12.0, 11.782, 'EUR/NOK below 12.000 — oil price correlation', 'INFO', '2026-03-19 11:00:00', NULL, NULL, 'OPEN'),
    (19, 7, 'SPIKE', NULL, 0.5812, 'NZD/USD spike down — RBNZ surprise cut', 'CRITICAL', '2026-03-18 22:00:05', '2026-03-18 22:05:00', 'apac.desk@hsbc.com', 'RESOLVED'),
    (20, 8, 'THRESHOLD_BREACH', 0.85, 0.8352, 'EUR/GBP failed to breach 0.8500 resistance', 'INFO', '2026-03-18 10:15:00', NULL, NULL, 'OPEN');
