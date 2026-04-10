-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Sample Data
--  Table : currency
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

INSERT INTO currency
    (currency_id, iso_code, currency_name, country, numeric_code, minor_units, is_active, region)
VALUES
    (1, 'USD', 'US Dollar', 'United States', '840', 2, TRUE, 'AMER'),
    (2, 'EUR', 'Euro', 'Euro Area', '978', 2, TRUE, 'EMEA'),
    (3, 'GBP', 'Pound Sterling', 'United Kingdom', '826', 2, TRUE, 'EMEA'),
    (4, 'JPY', 'Japanese Yen', 'Japan', '392', 0, TRUE, 'APAC'),
    (5, 'CHF', 'Swiss Franc', 'Switzerland', '756', 2, TRUE, 'EMEA'),
    (6, 'AUD', 'Australian Dollar', 'Australia', '036', 2, TRUE, 'APAC'),
    (7, 'CAD', 'Canadian Dollar', 'Canada', '124', 2, TRUE, 'AMER'),
    (8, 'NZD', 'New Zealand Dollar', 'New Zealand', '554', 2, TRUE, 'APAC'),
    (9, 'HKD', 'Hong Kong Dollar', 'Hong Kong', '344', 2, TRUE, 'APAC'),
    (10, 'SGD', 'Singapore Dollar', 'Singapore', '702', 2, TRUE, 'APAC'),
    (11, 'NOK', 'Norwegian Krone', 'Norway', '578', 2, TRUE, 'EMEA'),
    (12, 'SEK', 'Swedish Krona', 'Sweden', '752', 2, TRUE, 'EMEA'),
    (13, 'DKK', 'Danish Krone', 'Denmark', '208', 2, TRUE, 'EMEA'),
    (14, 'CNH', 'Chinese Yuan (Offshore)', 'China', '156', 2, TRUE, 'APAC'),
    (15, 'INR', 'Indian Rupee', 'India', '356', 2, TRUE, 'APAC'),
    (16, 'KRW', 'South Korean Won', 'South Korea', '410', 0, TRUE, 'APAC'),
    (17, 'MXN', 'Mexican Peso', 'Mexico', '484', 2, TRUE, 'AMER'),
    (18, 'BRL', 'Brazilian Real', 'Brazil', '986', 2, TRUE, 'AMER'),
    (19, 'ZAR', 'South African Rand', 'South Africa', '710', 2, TRUE, 'EMEA'),
    (20, 'PLN', 'Polish Zloty', 'Poland', '985', 2, TRUE, 'EMEA');
