-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Sample Data
--  Table : currency_pair
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

INSERT INTO currency_pair
    (pair_id, pair_code, base_currency, quote_currency, pair_type, decimal_places, pip_size, is_active)
VALUES
    (1, 'EUR/USD', 'EUR', 'USD', 'MAJOR', 4, 0.0001, TRUE),
    (2, 'GBP/USD', 'GBP', 'USD', 'MAJOR', 4, 0.0001, TRUE),
    (3, 'USD/JPY', 'USD', 'JPY', 'MAJOR', 2, 0.01, TRUE),
    (4, 'USD/CHF', 'USD', 'CHF', 'MAJOR', 4, 0.0001, TRUE),
    (5, 'AUD/USD', 'AUD', 'USD', 'MAJOR', 4, 0.0001, TRUE),
    (6, 'USD/CAD', 'USD', 'CAD', 'MAJOR', 4, 0.0001, TRUE),
    (7, 'NZD/USD', 'NZD', 'USD', 'MAJOR', 4, 0.0001, TRUE),
    (8, 'EUR/GBP', 'EUR', 'GBP', 'MINOR', 4, 0.0001, TRUE),
    (9, 'EUR/JPY', 'EUR', 'JPY', 'MINOR', 2, 0.01, TRUE),
    (10, 'GBP/JPY', 'GBP', 'JPY', 'MINOR', 2, 0.01, TRUE),
    (11, 'EUR/CHF', 'EUR', 'CHF', 'MINOR', 4, 0.0001, TRUE),
    (12, 'GBP/CHF', 'GBP', 'CHF', 'MINOR', 4, 0.0001, TRUE),
    (13, 'AUD/JPY', 'AUD', 'JPY', 'CROSS', 2, 0.01, TRUE),
    (14, 'USD/HKD', 'USD', 'HKD', 'MINOR', 4, 0.0001, TRUE),
    (15, 'USD/SGD', 'USD', 'SGD', 'MINOR', 4, 0.0001, TRUE),
    (16, 'USD/CNH', 'USD', 'CNH', 'MINOR', 4, 0.0001, TRUE),
    (17, 'USD/INR', 'USD', 'INR', 'EXOTIC', 4, 0.0001, TRUE),
    (18, 'EUR/NOK', 'EUR', 'NOK', 'MINOR', 4, 0.0001, TRUE),
    (19, 'USD/MXN', 'USD', 'MXN', 'EXOTIC', 4, 0.0001, TRUE),
    (20, 'USD/ZAR', 'USD', 'ZAR', 'EXOTIC', 4, 0.0001, TRUE);
