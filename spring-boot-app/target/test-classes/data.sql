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

INSERT INTO exchange_rate
    (rate_id, pair_id, provider_id, bid_rate, ask_rate, mid_rate, rate_timestamp, source_system, is_valid)
VALUES
    (1, 1, 1, 1.08312, 1.08318, 1.08315, '2026-03-26 08:00:01', 'REUTERS', TRUE),
    (2, 2, 1, 1.29621, 1.29629, 1.29625, '2026-03-26 08:00:02', 'REUTERS', TRUE),
    (3, 3, 2, 149.872, 149.878, 149.875, '2026-03-26 08:00:03', 'BLOOMBERG', TRUE),
    (4, 4, 2, 0.90124, 0.9013, 0.90127, '2026-03-26 08:00:04', 'BLOOMBERG', TRUE),
    (5, 5, 1, 0.63542, 0.63548, 0.63545, '2026-03-26 08:00:05', 'REUTERS', TRUE),
    (6, 6, 1, 1.36714, 1.3672, 1.36717, '2026-03-26 08:00:06', 'REUTERS', TRUE),
    (7, 7, 2, 0.58921, 0.58929, 0.58925, '2026-03-26 08:00:07', 'BLOOMBERG', TRUE),
    (8, 8, 8, 0.83512, 0.83518, 0.83515, '2026-03-26 08:00:08', 'WMR', TRUE),
    (9, 9, 8, 162.245, 162.255, 162.25, '2026-03-26 08:00:09', 'WMR', TRUE),
    (10, 10, 10, 194.181, 194.193, 194.187, '2026-03-26 08:00:10', 'ICAP', TRUE),
    (11, 11, 10, 0.97422, 0.97428, 0.97425, '2026-03-26 08:00:11', 'ICAP', TRUE),
    (12, 12, 14, 1.16542, 1.16554, 1.16548, '2026-03-26 08:00:12', 'HSBC_INT', TRUE),
    (13, 13, 2, 95.4211, 95.4293, 95.4252, '2026-03-26 08:00:13', 'BLOOMBERG', TRUE),
    (14, 14, 14, 7.82541, 7.82547, 7.82544, '2026-03-26 08:00:14', 'HSBC_INT', TRUE),
    (15, 15, 14, 1.34821, 1.34827, 1.34824, '2026-03-26 08:00:15', 'HSBC_INT', TRUE),
    (16, 16, 2, 7.23412, 7.23424, 7.23418, '2026-03-26 08:00:16', 'BLOOMBERG', TRUE),
    (17, 17, 1, 83.6241, 83.6289, 83.6265, '2026-03-26 08:00:17', 'REUTERS', TRUE),
    (18, 18, 1, 11.7821, 11.7833, 11.7827, '2026-03-26 08:00:18', 'REUTERS', TRUE),
    (19, 19, 2, 20.2142, 20.2164, 20.2153, '2026-03-26 08:00:19', 'BLOOMBERG', TRUE),
    (20, 20, 1, 18.8821, 18.8847, 18.8834, '2026-03-26 08:00:20', 'REUTERS', TRUE);
