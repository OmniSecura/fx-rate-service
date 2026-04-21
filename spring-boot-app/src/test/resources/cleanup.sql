-- ============================================================
-- cleanup.sql — run before each IT class to restore seed state
--
-- Removes any rows written by previous test classes (e.g. via POST /api/rates).
-- Seed data rows have rate_timestamp = '2026-03-26 ...' so we delete anything newer.
-- Tables that are never written to by tests (currency, currency_pair, rate_provider,
-- eod_fixing, forward_rate, rate_alert, rate_audit_log) are left untouched.
-- ============================================================

DELETE FROM exchange_rate
WHERE rate_timestamp > '2026-03-26 23:59:59';

