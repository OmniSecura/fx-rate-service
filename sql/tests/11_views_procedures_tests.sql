-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — View & Procedure Test Examples
--
--  Prerequisites (run in order before this file):
--    00_ddl_schema.sql
--    01_data_currencies.sql
--    02_data_rate_providers.sql
--    03_data_currency_pairs.sql
--    04_data_exchange_rates.sql
--    05_data_eod_fixings.sql
--    06_data_forward_rates.sql
--    07_data_rate_alerts.sql
--    08_data_rate_audit_log.sql
--    09_view_procedure.sql
--
--  Sections:
--    S1 — latest_rates_vw
--    S2 — rate_history_vw
--    S3 — store_rate  (procedure)
--    S4 — store_fixing (procedure)
--    S5 — get_rate    (procedure)
--    S6 — get_cross_rate (procedure)
--
--  DML tests (S3, S4) are wrapped in START TRANSACTION / ROLLBACK
--  so they leave the database unchanged after running.
-- ============================================================


-- ============================================================
--  S1. latest_rates_vw
--
--  View definition (09_view_procedure.sql):
--    Returns the most recent valid spot rate for every active
--    currency pair together with:
--      hours_since_update  — age of the rate in whole hours
--      is_stale            — 1 if hours_since_update > 4, else 0
-- ============================================================

-- T1.1  All active pairs — basic sanity check
--  EXPECT: 20 rows, one per active currency pair.
--          hours_since_update > 4 and is_stale = 1 for all rows
--          (sample data is from 2026-03-26, well over 4 h ago).
SELECT
    pair_code,
    pair_type,
    mid_rate,
    rate_timestamp,
    hours_since_update,
    is_stale
FROM latest_rates_vw
ORDER BY pair_code;


-- T1.2  Stale rates only
--  EXPECT: Same 20 rows as T1.1 — all sample rates are stale.
--          If 0 rows appear the staleness logic is broken.
SELECT
    pair_code,
    mid_rate,
    hours_since_update
FROM latest_rates_vw
WHERE is_stale = 1
ORDER BY hours_since_update DESC;


-- T1.3  Filter by pair type — MAJOR pairs only
--  EXPECT: 7 rows (AUD/USD, EUR/USD, GBP/USD, NZD/USD,
--          USD/CAD, USD/CHF, USD/JPY).
SELECT
    pair_code,
    pair_type,
    mid_rate,
    is_stale
FROM latest_rates_vw
WHERE pair_type = 'MAJOR'
ORDER BY pair_code;


-- T1.4  Staleness summary — count stale vs fresh
--  EXPECT: is_stale=1 count=20, is_stale=0 count=0
--          (all sample data is old).
SELECT
    is_stale,
    COUNT(*) AS pair_count
FROM latest_rates_vw
GROUP BY is_stale;


-- ============================================================
--  S2. rate_history_vw
--
--  View definition (09_view_procedure.sql):
--    Full history of all exchange_rate rows joined with
--    currency_pair and rate_provider.  Pre-computes:
--      spread = ROUND(ask_rate - bid_rate, 6)
--    Ordered newest first.
-- ============================================================

-- T2.1  Latest 10 rates — newest first
--  EXPECT: 10 rows from 2026-03-26.
--          spread = ask_rate - bid_rate for every row.
SELECT
    rate_id,
    pair_code,
    provider_code,
    bid_rate,
    ask_rate,
    mid_rate,
    spread,
    rate_timestamp
FROM rate_history_vw
LIMIT 10;


-- T2.2  Full history for a single pair
--  EXPECT: 1 row (rate_id = 1), is_valid = TRUE, pair_code = 'EUR/USD'.
SELECT
    rate_id,
    pair_code,
    provider_name,
    bid_rate,
    ask_rate,
    mid_rate,
    spread,
    rate_timestamp,
    is_valid
FROM rate_history_vw
WHERE pair_code = 'EUR/USD';


-- T2.3  Valid rates ranked by spread size — widest first
--  EXPECT: Exotic pairs (USD/INR, USD/ZAR, USD/MXN) appear at the top.
--          Major pairs (EUR/USD, GBP/USD) appear near the bottom.
SELECT
    pair_code,
    pair_type,
    bid_rate,
    ask_rate,
    mid_rate,
    spread
FROM rate_history_vw
WHERE is_valid = TRUE
ORDER BY spread DESC;


-- T2.4  Rate count per provider
--  EXPECT: REUTERS and BLOOMBERG appear as top providers.
SELECT
    provider_code,
    provider_name,
    COUNT(*) AS rate_count
FROM rate_history_vw
GROUP BY provider_code, provider_name
ORDER BY rate_count DESC;


-- T2.5  All rates on a specific date
--  EXPECT: 20 rows, all timestamped 2026-03-26.
SELECT
    pair_code,
    mid_rate,
    rate_timestamp,
    source_system
FROM rate_history_vw
WHERE DATE(rate_timestamp) = '2026-03-26'
ORDER BY rate_timestamp;


-- ============================================================
--  S3. store_rate procedure
--
--  Signature:
--    CALL store_rate(p_rate_id, p_pair_id, p_provider_id,
--                   p_bid_rate, p_ask_rate,
--                   p_rate_timestamp, p_source_system)
--
--  Behaviour:
--    1. Raises SQLSTATE 45000 if bid > ask.
--    2. Computes mid_rate = ROUND((bid+ask)/2, 6).
--    3. Marks all previous valid+non-stale rates for the pair
--       as is_stale = TRUE.
--    4. Inserts the new rate with is_valid = TRUE, is_stale = FALSE.
-- ============================================================

-- T3.1  Valid insert — mid auto-calculated, old rate marked stale
--  EXPECT: New row rate_id=21, mid_rate=1.083550, is_stale=FALSE.
--          Old row rate_id=1 (pair_id=1) should now have is_stale=TRUE.
START TRANSACTION;

CALL store_rate(21, 1, 1, 1.08350, 1.08360, NOW(), 'REUTERS');

-- Verify new row
SELECT
    rate_id,
    pair_id,
    bid_rate,
    ask_rate,
    mid_rate,
    is_valid,
    is_stale
FROM exchange_rate
WHERE rate_id = 21;

-- Verify old row is now stale
SELECT
    rate_id,
    pair_id,
    mid_rate,
    is_stale
FROM exchange_rate
WHERE pair_id = 1
ORDER BY rate_id;

ROLLBACK;


-- T3.2  bid > ask — procedure must raise an error
--  EXPECT: ERROR 1644 (SQLSTATE 45000)
--          Message: 'Invalid rate: bid_rate cannot be greater than ask_rate'
--          No row inserted.
START TRANSACTION;

CALL store_rate(22, 2, 1, 1.29700, 1.29600, NOW(), 'REUTERS');

ROLLBACK;


-- T3.3  Insert for USD/JPY (pair_id=3) — JPY pip size = 0.01
--  EXPECT: rate_id=23, mid_rate=150.020000.
START TRANSACTION;

CALL store_rate(23, 3, 2, 150.010, 150.030, NOW(), 'BLOOMBERG');

SELECT
    rate_id,
    bid_rate,
    ask_rate,
    mid_rate
FROM exchange_rate
WHERE rate_id = 23;

ROLLBACK;


-- T3.4  Insert for exotic pair USD/INR (pair_id=17)
--  EXPECT: rate_id=24, mid_rate=84.000000, previous rate is_stale=TRUE.
START TRANSACTION;

CALL store_rate(24, 17, 1, 83.990, 84.010, NOW(), 'REUTERS');

SELECT rate_id, bid_rate, ask_rate, mid_rate, is_stale
FROM exchange_rate
WHERE pair_id = 17
ORDER BY rate_id;

ROLLBACK;


-- ============================================================
--  S4. store_fixing procedure
--
--  Signature:
--    CALL store_fixing(p_fixing_id, p_pair_id, p_provider_id,
--                      p_fixing_date, p_fixing_rate,
--                      p_fixing_time, p_fixing_type,
--                      p_is_official, p_published_at,
--                      p_threshold)
--
--  Behaviour:
--    1. Looks up the last valid mid_rate for the pair.
--    2. If deviation > threshold (default 1%), inserts a
--       WARNING row into rate_alert.
--    3. Inserts the fixing into eod_fixing regardless.
-- ============================================================

-- T4.1  Normal fixing — deviation < 1% → no alert generated
--  fixing_rate=1.08315 vs last mid=1.08315 → 0% deviation.
--  EXPECT: fixing inserted, no new alert for pair_id=1.
START TRANSACTION;

CALL store_fixing(21, 1, 8, '2026-04-08', 1.08315, '16:00 LON', 'WMR', TRUE, NOW(), NULL);

SELECT fixing_id, pair_id, fixing_rate, fixing_type, is_official
FROM eod_fixing
WHERE fixing_id = 21;

SELECT COUNT(*) AS new_alerts
FROM rate_alert
WHERE pair_id = 1;

ROLLBACK;


-- T4.2  Large deviation (>1%) → alert generated
--  fixing_rate=160.00 vs last mid≈149.875 → ~6.76% deviation > 1%.
--  EXPECT: fixing inserted AND one WARNING alert for pair_id=3.
START TRANSACTION;

CALL store_fixing(22, 3, 2, '2026-04-08', 160.00, '11:00 ECB', 'ECB', TRUE, NOW(), 0.01);

SELECT fixing_id, pair_id, fixing_rate, fixing_type
FROM eod_fixing
WHERE fixing_id = 22;

SELECT pair_id, alert_type, severity, actual_value, alert_message
FROM rate_alert
WHERE pair_id = 3
ORDER BY alert_id DESC
LIMIT 1;

ROLLBACK;


-- T4.3  Custom tight threshold (0.5%) — moderate deviation triggers alert
--  fixing_rate=1.0900 vs last mid≈1.08315 → ~0.63% deviation > 0.5%.
--  EXPECT: fixing inserted AND one WARNING alert for pair_id=1.
START TRANSACTION;

CALL store_fixing(23, 1, 8, '2026-04-09', 1.09000, '16:00 LON', 'WMR', TRUE, NOW(), 0.005);

SELECT pair_id, alert_type, severity, threshold_value, actual_value
FROM rate_alert
WHERE pair_id = 1
ORDER BY alert_id DESC
LIMIT 1;

ROLLBACK;


-- ============================================================
--  S5. get_rate procedure
--
--  Signature:
--    CALL get_rate(p_pair_code, p_stale_minutes)
--
--  Behaviour:
--    Returns the latest valid rate for p_pair_code.
--    is_stale = TRUE if rate_timestamp is older than
--    p_stale_minutes (NULL defaults to 60 minutes).
-- ============================================================

-- T5.1  Known active pair — 60-minute staleness window
--  EXPECT: pair_code=EUR/USD, mid_rate=1.08315,
--          is_stale=TRUE (rate from 2026-03-26),
--          age_minutes >> 60.
CALL get_rate('EUR/USD', 60);


-- T5.2  NULL threshold — defaults to 60 minutes
--  EXPECT: same staleness result as T5.1 for GBP/USD.
CALL get_rate('GBP/USD', NULL);


-- T5.3  Very large threshold — rate should appear fresh
--  EXPECT: is_stale=FALSE (threshold > actual age).
--          This verifies the threshold parameter is respected.
CALL get_rate('EUR/USD', 999999);


-- T5.4  Exotic pair
--  EXPECT: pair_code=USD/INR, mid_rate=83.6265, is_stale=TRUE.
CALL get_rate('USD/INR', 30);


-- T5.5  Unknown pair code — no row returned
--  EXPECT: empty result set (0 rows).
CALL get_rate('XXX/YYY', 60);


-- ============================================================
--  S6. get_cross_rate procedure
--
--  Signature:
--    CALL get_cross_rate(p_pair1, p_pair2, p_stale_minutes)
--
--  Behaviour:
--    Finds the common currency between p_pair1 and p_pair2,
--    then computes the implied cross rate.
--    Returns NULL cross_rate when no common currency exists
--    or when either component rate is missing.
-- ============================================================

-- T6.1  EUR/JPY implied from EUR/USD × USD/JPY  (common: USD as quote1/base2)
--  EXPECT: cross_pair='EUR/JPY',
--          cross_rate ≈ 1.08315 × 149.875 ≈ 162.34,
--          is_stale1=TRUE, is_stale2=TRUE.
CALL get_cross_rate('EUR/USD', 'USD/JPY', 60);


-- T6.2  GBP/JPY implied from GBP/USD × USD/JPY
--  EXPECT: cross_pair='GBP/JPY',
--          cross_rate ≈ 1.29625 × 149.875 ≈ 194.24.
CALL get_cross_rate('GBP/USD', 'USD/JPY', 60);


-- T6.3  EUR/CHF implied from EUR/USD and USD/CHF  (common: USD)
--  EXPECT: cross_pair='EUR/CHF',
--          cross_rate ≈ 1.08315 × 0.90127 ≈ 0.9764.
CALL get_cross_rate('EUR/USD', 'USD/CHF', 60);


-- T6.4  AUD/JPY implied from AUD/USD × USD/JPY
--  EXPECT: cross_pair='AUD/JPY',
--          cross_rate ≈ 0.63545 × 149.875 ≈ 95.24.
--          (This pair already exists as pair_id=13 — useful sanity check.)
CALL get_cross_rate('AUD/USD', 'USD/JPY', 60);


-- T6.5  No common currency — NULL result
--  EUR/USD and GBP/CHF share no currency.
--  EXPECT: cross_pair=NULL, cross_rate=NULL.
CALL get_cross_rate('EUR/USD', 'GBP/CHF', 60);


-- T6.6  Very large staleness threshold — both component rates appear fresh
--  EXPECT: is_stale1=FALSE, is_stale2=FALSE.
CALL get_cross_rate('EUR/USD', 'USD/JPY', 999999);

