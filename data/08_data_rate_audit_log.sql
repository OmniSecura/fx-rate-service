-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Sample Data
--  Table : rate_audit_log
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

INSERT INTO rate_audit_log
    (log_id, rate_id, pair_id, action, old_mid_rate, new_mid_rate, change_pct, changed_by, changed_at, reason)
VALUES
    (1, 1, 1, 'INSERT', NULL, 1.08315, NULL, 'feed.ingest', '2026-03-26 08:00:01', 'Initial load from Reuters'),
    (2, 3, 3, 'INSERT', NULL, 149.875, NULL, 'feed.ingest', '2026-03-26 08:00:03', 'Initial load from Bloomberg'),
    (3, 17, 17, 'INSERT', NULL, 83.6265, NULL, 'feed.ingest', '2026-03-26 08:00:17', 'Initial load from Reuters'),
    (4, 1, 1, 'UPDATE', 1.08315, 1.08342, 0.0249, 'feed.ingest', '2026-03-26 08:15:00', 'Tick update'),
    (5, 3, 3, 'UPDATE', 149.875, 150.212, 0.2249, 'feed.ingest', '2026-03-26 04:12:08', 'Spike detection — large move'),
    (6, 3, 3, 'INVALIDATE', 150.212, NULL, NULL, 'risk.control', '2026-03-26 04:12:15', 'Rate spike invalidated pending review'),
    (7, 3, 3, 'UPDATE', NULL, 149.921, NULL, 'risk.control', '2026-03-26 04:18:44', 'Corrected rate reloaded after review'),
    (8, 6, 6, 'INSERT', NULL, 1.36717, NULL, 'feed.ingest', '2026-03-26 08:00:06', 'Initial load from Reuters'),
    (9, 6, 6, 'UPDATE', 1.36717, 1.36698, -0.0139, 'feed.ingest', '2026-03-26 09:00:00', 'Tick update'),
    (10, 19, 19, 'INSERT', NULL, 20.2153, NULL, 'feed.ingest', '2026-03-26 08:00:19', 'Initial load from Bloomberg'),
    (11, 19, 19, 'UPDATE', 20.2153, 20.65, 2.1459, 'feed.ingest', '2026-03-25 14:22:31', 'Spike — possible fat-finger'),
    (12, 19, 19, 'INVALIDATE', 20.65, NULL, NULL, 'risk.control', '2026-03-25 14:22:35', 'Fat finger invalidated'),
    (13, 19, 19, 'UPDATE', NULL, 20.2088, NULL, 'risk.control', '2026-03-25 14:25:00', 'Corrected rate'),
    (14, 2, 2, 'INSERT', NULL, 1.29625, NULL, 'feed.ingest', '2026-03-26 08:00:02', 'Initial load from Reuters'),
    (15, 2, 2, 'UPDATE', 1.29625, 1.29598, -0.0209, 'feed.ingest', '2026-03-25 16:59:55', 'EOD tick'),
    (16, 2, 2, 'INVALIDATE', 1.29598, NULL, NULL, 'feed.ingest', '2026-03-25 16:59:55', 'Stale rate — no provider update'),
    (17, 7, 7, 'INSERT', NULL, 0.58925, NULL, 'feed.ingest', '2026-03-26 08:00:07', 'Initial load from Bloomberg'),
    (18, 7, 7, 'UPDATE', 0.58925, 0.5812, -1.3666, 'feed.ingest', '2026-03-18 22:00:05', 'RBNZ surprise rate cut'),
    (19, 20, 20, 'INSERT', NULL, 18.8834, NULL, 'feed.ingest', '2026-03-26 08:00:20', 'Initial load from Reuters'),
    (20, 20, 20, 'UPDATE', 18.8834, 18.8962, 0.0677, 'feed.ingest', '2026-03-26 09:30:00', 'EM session update');
