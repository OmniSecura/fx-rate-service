-- ============================================================
--  HSBC Graduate Programme 2026 — Project 03
--  FX Rate Service — Query Validation
--
--  Covers:
--    Q1 — Stale rate detection (timestamp + validity)
--    Q2 — Bid/ask spread calculation
--    Q3 — Forward rate aggregation
--    Q4 — Rate audit trail
-- ============================================================


-- ------------------------------------------------------------
-- Q1. Stale rate detection
--
--  WHAT IT VALIDATES:
--    Finds all active currency pairs whose most recent VALID rate
--    has not been updated in the last 4 hours.
--    Uses the latest_rates_vw view (defined in 09_view_procedure.sql)
--    which encapsulates is_valid logic and calculates is_stale flag.
--
--  HOW TO INTERPRET RESULTS:
--    ✓ PASS — query runs without error and returns rows for pairs
--             whose rates are genuinely old (e.g. loaded in March 2026).
--             hours_since_update should be > 4 for every row returned.
--    ✗ PROBLEM — if hours_since_update <= 4 in any row, the view's
--             stale logic is wrong.
--    ✗ PROBLEM — if no rows appear at all with sample data (all dated
--             2026-03-26), something is wrong with the timestamp logic.
-- ------------------------------------------------------------
SELECT
    pair_code,
    pair_type,
    mid_rate,
    rate_timestamp                AS last_updated,
    hours_since_update            AS age_hours
FROM latest_rates_vw
WHERE is_stale = 1
ORDER BY rate_timestamp ASC;


-- ------------------------------------------------------------
-- Q2. Bid/ask spread calculation
--
--  WHAT IT VALIDATES:
--    Calculates the spread (ask - bid) for every currently valid rate,
--    expressed both as a raw decimal and in pips (using pip_size per pair).
--    Checks that bid <= mid <= ask (the fundamental FX pricing rule).
--    Guards against NULL rates and zero pip_size.
--    Uses rate_history_vw (defined in 09_view_procedure.sql) which
--    pre-computes spread and joins pair + provider details.
--
--  HOW TO INTERPRET RESULTS:
--    ✓ PASS — all rows show spread_status = 'OK'.
--             spread > 0 for every row.
--             spread_pips is larger for exotic pairs (USD/INR, USD/ZAR)
--             and smaller for majors (EUR/USD, GBP/USD).
--    ✗ PROBLEM — 'INVALID' means bid > ask — data or constraint violation.
--    ✗ PROBLEM — 'MID_OUTSIDE_SPREAD' means mid is not between bid and ask.
--    ✗ PROBLEM — 'INCOMPLETE' means a rate column is unexpectedly NULL.
--    ✗ PROBLEM — spread_pips = NULL means pip_size is 0 (schema error).
-- ------------------------------------------------------------
SELECT
    rh.pair_code,
    rh.pair_type,
    rh.bid_rate,
    rh.ask_rate,
    rh.mid_rate,
    rh.spread,
    ROUND(rh.spread / NULLIF(cp.pip_size, 0), 2)                              AS spread_pips,
    CASE
        WHEN rh.bid_rate IS NULL OR rh.ask_rate IS NULL  THEN 'INCOMPLETE'
        WHEN rh.bid_rate > rh.ask_rate                   THEN 'INVALID'
        WHEN rh.mid_rate NOT BETWEEN rh.bid_rate
                                 AND rh.ask_rate         THEN 'MID_OUTSIDE_SPREAD'
        ELSE                                                  'OK'
    END                                                                       AS spread_status
FROM rate_history_vw rh
JOIN currency_pair cp ON cp.pair_code = rh.pair_code
WHERE rh.is_valid = TRUE
ORDER BY spread_pips DESC;


-- ------------------------------------------------------------
-- Q3. Forward rate aggregation
--
--  WHAT IT VALIDATES:
--    Aggregates all forward rates per currency pair and tenor.
--    Checks that forward points can be negative (discount) or positive
--    (premium), and that avg/min/max are computed correctly across tenors.
--    Uses GROUP BY — tests aggregation logic.
--
--  HOW TO INTERPRET RESULTS:
--    ✓ PASS — EUR/USD rows show curve_direction = 'DISCOUNT'
--             (forward_points are negative — USD interest rates > EUR).
--             USD/JPY rows show curve_direction = 'PREMIUM'
--             (forward_points are positive — JPY rates < USD rates).
--             Every pair with forward data appears at least once.
--    ✗ PROBLEM — curve_direction = 'FLAT' on a major pair likely means
--             forward_points are all zero (bad sample data).
--    ✗ PROBLEM — avg_forward_points = NULL means forward_points column
--             contains unexpected NULLs (schema allows NOT NULL).
--    ✗ PROBLEM — missing pairs mean the JOIN or GROUP BY is filtering
--             rows incorrectly.
-- ------------------------------------------------------------
SELECT
    cp.pair_code,
    fr.tenor,
    COUNT(*)                               AS rate_count,
    ROUND(AVG(fr.forward_points), 4)       AS avg_forward_points,
    ROUND(MIN(fr.forward_points), 4)       AS min_forward_points,
    ROUND(MAX(fr.forward_points), 4)       AS max_forward_points,
    ROUND(AVG(fr.forward_rate),   6)       AS avg_forward_rate,
    CASE
        WHEN AVG(fr.forward_points) > 0 THEN 'PREMIUM'
        WHEN AVG(fr.forward_points) < 0 THEN 'DISCOUNT'
        ELSE                                 'FLAT'
    END                                    AS curve_direction
FROM forward_rate fr
JOIN currency_pair cp ON cp.pair_id = fr.pair_id
GROUP BY cp.pair_id, cp.pair_code, fr.tenor
ORDER BY cp.pair_code, fr.tenor;


-- ------------------------------------------------------------
-- Q4. Rate audit trail
--
--  WHAT IT VALIDATES:
--    Produces a complete, chronologically ordered change history
--    for every rate. Covers INSERT (new rate), UPDATE (tick change),
--    and INVALIDATE (rate removed/corrected) actions.
--    COALESCE replaces NULL old/new/change values with 'n/a'
--    so the trail is always readable.
--
--  HOW TO INTERPRET RESULTS:
--    ✓ PASS — USD/JPY trail shows: INSERT → UPDATE (spike) → INVALIDATE
--             → UPDATE (corrected). This matches the spike scenario
--             in the sample data.
--             USD/MXN trail shows: INSERT → UPDATE → INVALIDATE → UPDATE.
--             GBP/USD trail shows: UPDATE → INVALIDATE on same timestamp,
--             then INSERT — ordered by log_id as tie-breaker.
--             old_rate = 'n/a' on every INSERT row (no previous value).
--             change_pct = 'n/a' on every INVALIDATE row (rate removed).
--    ✗ PROBLEM — INSERT rows showing a non-null old_rate indicate
--             corrupt audit data.
--    ✗ PROBLEM — rows out of chronological order mean ORDER BY is wrong.
--    ✗ PROBLEM — NULL appearing instead of 'n/a' means COALESCE is missing.
-- ------------------------------------------------------------
SELECT
    cp.pair_code,
    ral.log_id,
    ral.action,
    COALESCE(CAST(ral.old_mid_rate AS CHAR), 'n/a')           AS old_rate,
    COALESCE(CAST(ral.new_mid_rate AS CHAR), 'n/a')           AS new_rate,
    COALESCE(CAST(ral.change_pct   AS CHAR), 'n/a')           AS change_pct,
    ral.changed_by,
    ral.changed_at,
    COALESCE(ral.reason, 'No reason recorded')                AS reason
FROM rate_audit_log ral
JOIN currency_pair cp ON cp.pair_id = ral.pair_id
ORDER BY cp.pair_code, ral.changed_at, ral.log_id;
