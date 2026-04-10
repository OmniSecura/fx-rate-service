-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Sample Data
--  Table : rate_provider
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

INSERT INTO rate_provider
    (provider_id, provider_code, provider_name, provider_type, country, is_active, priority)
VALUES
    (1, 'REUTERS', 'Refinitiv (Reuters) Eikon', 'MARKET_DATA', 'GB', TRUE, 1),
    (2, 'BLOOMBERG', 'Bloomberg Terminal FX', 'MARKET_DATA', 'US', TRUE, 1),
    (3, 'ECB', 'European Central Bank', 'CENTRAL_BANK', 'DE', TRUE, 2),
    (4, 'BOE', 'Bank of England', 'CENTRAL_BANK', 'GB', TRUE, 2),
    (5, 'FED', 'US Federal Reserve', 'CENTRAL_BANK', 'US', TRUE, 2),
    (6, 'BOFJ', 'Bank of Japan', 'CENTRAL_BANK', 'JP', TRUE, 2),
    (7, 'SNB', 'Swiss National Bank', 'CENTRAL_BANK', 'CH', TRUE, 2),
    (8, 'WMR', 'WM/Reuters 4pm London Fix', 'MARKET_DATA', 'GB', TRUE, 1),
    (9, 'BFIX', 'Bloomberg BFIX Benchmark', 'MARKET_DATA', 'US', TRUE, 1),
    (10, 'ICAP', 'ICAP EBS FX Platform', 'BROKER', 'GB', TRUE, 1),
    (11, 'CURRENEX', 'State Street Currenex', 'BROKER', 'US', TRUE, 2),
    (12, 'FXC', 'FXConnect (State Street)', 'BROKER', 'US', TRUE, 2),
    (13, 'RTFX', 'Refinitiv FXall', 'BROKER', 'GB', TRUE, 2),
    (14, 'HSBC_INT', 'HSBC Internal Rate Engine', 'INTERNAL', 'GB', TRUE, 1),
    (15, 'FXCM', 'FXCM Institutional', 'BROKER', 'US', TRUE, 3),
    (16, 'OANDA', 'OANDA Rate API', 'MARKET_DATA', 'US', TRUE, 3),
    (17, 'XE', 'XE.com Corporate FX', 'MARKET_DATA', 'CA', TRUE, 3),
    (18, 'MAS', 'Monetary Authority of Singapore', 'CENTRAL_BANK', 'SG', TRUE, 2),
    (19, 'RBA', 'Reserve Bank of Australia', 'CENTRAL_BANK', 'AU', TRUE, 2),
    (20, 'HKMA', 'Hong Kong Monetary Authority', 'CENTRAL_BANK', 'HK', TRUE, 2);
