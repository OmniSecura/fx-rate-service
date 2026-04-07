-- ============================================================
--  FX Rate Service — Business Queries & Validation
--  File   : Data/09_queries_validation.sql
--  Target : MySQL 8  |  Database: fx_rate_db
--
--  Covers requirement 1.3:
--    – stale-rate detection  (timestamp / validity logic)
--    – bid/ask spread        (calculation + analysis)
--    – forward-rate          (aggregation + curve)
--    – rate audit trail      (change history + window functions)
--    – NULL / edge cases     (COALESCE, NULLIF, safe division)
--    – business sanity       (data quality spot checks)
--
--  READ-ONLY — this file never modifies schema or data.
--  Adjust @stale_hours to change the staleness threshold.
-- ============================================================

USE fx_rate_db;

SET @stale_hours = 4;

-- ============================================================
--  SECTION 0 — HELPER VIEW : latest_rates_vw
--  Provides the single most-recent valid rate per currency pair.
--  ROW_NUMBER ensures we pick exactly one row even if a pair
--  has multiple rates at the same timestamp.
--  Several queries below build on this view.
-- ============================================================

CREATE OR REPLACE VIEW latest_rates_vw AS
WITH ranked_rates AS (
    SELECT
        er.rate_id,
        er.pair_id,
        er.provider_id,
        er.bid_rate,
        er.ask_rate,
        er.mid_rate,
        er.rate_timestamp,
        er.source_system,
        ROW_NUMBER() OVER (
            PARTITION BY er.pair_id
            ORDER BY er.rate_timestamp DESC, er.rate_id DESC
        ) AS rn
    FROM exchange_rate er
    WHERE er.is_valid = TRUE
)
SELECT
    rate_id,
    pair_id,
    provider_id,
    bid_rate,
    ask_rate,
    mid_rate,
    rate_timestamp,
    source_system
FROM ranked_rates
WHERE rn = 1;


-- ============================================================
--  SECTION 1 — CURRENT SPOT RATES WITH SPREAD
--  Business question: "What is the live market rate for each pair?"
--  Shows bid, ask, mid and the cost of trading (spread).
-- ============================================================

SELECT
    cp.pair_code,
    cp.pair_type,
    rp.provider_code,
    lr.bid_rate,
    lr.ask_rate,
    lr.mid_rate,
    ROUND(lr.ask_rate - lr.bid_rate, 6)   AS spread,
    lr.rate_timestamp,
    lr.source_system
FROM       latest_rates_vw lr
JOIN currency_pair cp  ON cp.pair_id     = lr.pair_id
JOIN rate_provider rp  ON rp.provider_id = lr.provider_id
WHERE cp.is_active = TRUE
ORDER BY cp.pair_type, cp.pair_code;


-- ============================================================
--  SECTION 2 — STALE RATE DETECTION
--  Business question: "Which pairs have not been refreshed recently?"
--  A rate is considered stale when older than @stale_hours.
--  CTE re-uses the view result so exchange_rate is scanned once.
-- ============================================================

WITH stale_candidates AS (
    SELECT
        lr.pair_id,
        lr.mid_rate,
        lr.rate_timestamp,
        TIMESTAMPDIFF(HOUR,   lr.rate_timestamp, NOW()) AS age_hours,
        TIMESTAMPDIFF(MINUTE, lr.rate_timestamp, NOW()) AS age_minutes
    FROM latest_rates_vw lr
)
SELECT
    cp.pair_code,
    cp.pair_type,
    sc.mid_rate,
    sc.rate_timestamp                                  AS last_updated,
    CONCAT(
        sc.age_hours, 'h ',
        MOD(sc.age_minutes, 60), 'm'
    )                                                  AS age
FROM stale_candidates sc
JOIN currency_pair cp ON cp.pair_id = sc.pair_id
WHERE cp.is_active  = TRUE
  AND sc.age_hours >= @stale_hours
ORDER BY sc.rate_timestamp ASC;


-- ============================================================
--  SECTION 3 — SPREAD ANALYSIS BY PAIR TYPE
--  Business question: "Are spreads healthy? Where is it most
--                      expensive to trade?"
--  Window function computes the average spread within each pair
--  type, letting us rank each pair against its own peer group.
-- ============================================================

SELECT
    cp.pair_code,
    cp.pair_type,
    ROUND(lr.ask_rate - lr.bid_rate, 6)                AS spread,
    ROUND(
        AVG(lr.ask_rate - lr.bid_rate)
            OVER (PARTITION BY cp.pair_type), 6
    )                                                  AS avg_spread_in_type,
    DENSE_RANK()
        OVER (
            PARTITION BY cp.pair_type
            ORDER BY (lr.ask_rate - lr.bid_rate) DESC
        )                                              AS spread_rank_in_type
FROM       latest_rates_vw lr
JOIN currency_pair cp ON cp.pair_id = lr.pair_id
WHERE cp.is_active = TRUE
ORDER BY cp.pair_type, spread_rank_in_type;


-- ============================================================
--  SECTION 4 — INTRADAY MID vs LATEST EOD FIXING
--  Business question: "How much has each rate moved since the
--                      last official close?"
--  CTE finds the latest available WMR fixing date per pair,
--  avoiding correlated subqueries (N+1 anti-pattern).
--  NULLIF prevents division-by-zero if fixing_rate is 0.
-- ============================================================

WITH latest_wmr AS (
    SELECT
        pair_id,
        MAX(fixing_date) AS latest_date
    FROM eod_fixing
    WHERE fixing_type = 'WMR'
    GROUP BY pair_id
)
SELECT
    cp.pair_code,
    lr.mid_rate                                        AS intraday_mid,
    ef.fixing_rate                                     AS last_wmr_fix,
    ef.fixing_date                                     AS fix_date,
    ROUND(
        (lr.mid_rate - ef.fixing_rate)
        / NULLIF(ef.fixing_rate, 0) * 100, 4
    )                                                  AS pct_change_from_fix
FROM       latest_rates_vw lr
JOIN currency_pair  cp  ON cp.pair_id      = lr.pair_id
JOIN latest_wmr     lw  ON lw.pair_id      = lr.pair_id
JOIN eod_fixing     ef  ON ef.pair_id      = lw.pair_id
                       AND ef.fixing_date  = lw.latest_date
                       AND ef.fixing_type  = 'WMR'
WHERE cp.is_active = TRUE
ORDER BY ABS(
    (lr.mid_rate - ef.fixing_rate) / NULLIF(ef.fixing_rate, 0)
) DESC;


-- ============================================================
--  SECTION 5 — FORWARD RATE CURVE AGGREGATION
--  Business question: "What does the forward market expect for
--                      each currency pair?"
--  Forward points can be negative (discount) or positive (premium).
-- ============================================================

-- 5A) Full curve: every tenor per pair in date order
SELECT
    cp.pair_code,
    fr.tenor,
    fr.value_date,
    fr.forward_points,
    fr.forward_rate,
    fr.rate_timestamp
FROM forward_rate  fr
JOIN currency_pair cp ON cp.pair_id = fr.pair_id
ORDER BY cp.pair_code, fr.value_date;

-- 5B) Summary per pair: stats + premium/discount tenor count
SELECT
    cp.pair_code,
    COUNT(*)                              AS tenor_count,
    ROUND(AVG(fr.forward_points), 4)      AS avg_fwd_points,
    ROUND(MIN(fr.forward_points), 4)      AS min_fwd_points,
    ROUND(MAX(fr.forward_points), 4)      AS max_fwd_points,
    SUM(fr.forward_points >  0)           AS tenors_at_premium,
    SUM(fr.forward_points <  0)           AS tenors_at_discount
FROM forward_rate  fr
JOIN currency_pair cp ON cp.pair_id = fr.pair_id
GROUP BY cp.pair_code
ORDER BY cp.pair_code;


-- ============================================================
--  SECTION 6 — RATE AUDIT TRAIL
--  Business question: "What changed, when, and by how much?"
--  LAG() retrieves the previous new_mid_rate in the same pair's
--  timeline so reviewers can follow rate history step by step.
--  COALESCE replaces NULL reason with a readable placeholder.
-- ============================================================

-- 6A) Full timeline per pair with change context
SELECT
    cp.pair_code,
    ral.log_id,
    ral.action,
    ral.old_mid_rate,
    ral.new_mid_rate,
    ral.change_pct                                     AS stored_pct,
    ROUND(
        (ral.new_mid_rate - ral.old_mid_rate)
        / NULLIF(ral.old_mid_rate, 0) * 100, 4
    )                                                  AS recalc_pct,
    ral.changed_by,
    ral.changed_at,
    COALESCE(ral.reason, '—')                          AS reason,
    LAG(ral.new_mid_rate) OVER (
        PARTITION BY ral.pair_id
        ORDER BY ral.changed_at, ral.log_id
    )                                                  AS prev_mid_rate
FROM rate_audit_log ral
JOIN currency_pair  cp ON cp.pair_id = ral.pair_id
ORDER BY cp.pair_code, ral.changed_at, ral.log_id;

-- 6B) Event summary: how many actions of each type
SELECT
    action,
    COUNT(*)                AS event_count,
    COUNT(DISTINCT pair_id) AS pairs_affected
FROM rate_audit_log
GROUP BY action
ORDER BY event_count DESC;


-- ============================================================
--  SECTION 7 — BUSINESS SANITY CHECKS
--  Three lightweight checks that flag potential data problems.
--  Expected result per check is noted in the comment above it.
-- ============================================================

-- 7A) Active pairs with NO valid exchange rate.
-- Expected: 0 rows — every active pair must have at least one rate.
SELECT
    cp.pair_code,
    cp.pair_type
FROM currency_pair cp
WHERE cp.is_active = TRUE
  AND NOT EXISTS (
        SELECT 1
        FROM   exchange_rate er
        WHERE  er.pair_id  = cp.pair_id
          AND  er.is_valid = TRUE
  );

-- 7B) Open alerts grouped by severity — review CRITICAL ones first.
SELECT
    ra.severity,
    ra.alert_type,
    COUNT(*)               AS open_count,
    MIN(ra.triggered_at)   AS oldest_open
FROM rate_alert ra
WHERE ra.status = 'OPEN'
GROUP BY ra.severity, ra.alert_type
ORDER BY
    CASE ra.severity
        WHEN 'CRITICAL' THEN 1
        WHEN 'WARNING'  THEN 2
        ELSE                 3
    END;

-- 7C) Same provider, same pair, same day — more than one valid rate.
-- May indicate a duplicate feed entry.
-- Expected: 0 rows for clean intraday data.
SELECT
    cp.pair_code,
    rp.provider_code,
    DATE(er.rate_timestamp)   AS rate_date,
    COUNT(*)                  AS rate_count
FROM exchange_rate er
JOIN currency_pair cp ON cp.pair_id     = er.pair_id
JOIN rate_provider rp ON rp.provider_id = er.provider_id
WHERE er.is_valid = TRUE
GROUP BY cp.pair_code, rp.provider_code, DATE(er.rate_timestamp)
HAVING rate_count > 1
ORDER BY rate_count DESC;

