# Project 03 — FX Rate Service: Sample Data

## Load order

Run `00_ddl_schema.sql` first to create all tables, then load data files in numbered order:

```
01_data_currencies.sql
02_data_rate_providers.sql
03_data_currency_pairs.sql
04_data_exchange_rates.sql
05_data_eod_fixings.sql
06_data_forward_rates.sql
07_data_rate_alerts.sql
08_data_rate_audit_log.sql
```

## Tables (20 rows each)

| File | Table | Description |
|------|-------|-------------|
| 01 | `currency` | 20 ISO 4217 currencies: G10 majors + key EM/APAC currencies |
| 02 | `rate_provider` | 20 rate sources: market data vendors, central banks, brokers, HSBC internal |
| 03 | `currency_pair` | 20 tradeable pairs: 7 majors, minors, crosses and exotics |
| 04 | `exchange_rate` | 20 intraday spot rates — London open 26 Mar 2026, bid/ask/mid |
| 05 | `eod_fixing` | 20 end-of-day fixings: WMR 4pm London, ECB 11am, BFIX — 25 Mar 2026 |
| 06 | `forward_rate` | 20 forward FX rates across tenors (1M to 1Y) for major pairs |
| 07 | `rate_alert` | 20 alerts: threshold breaches, stale rates, spread widening, spikes |
| 08 | `rate_audit_log` | 20 audit records: INSERT / UPDATE / INVALIDATE actions with reasons |

## Compatibility

| Feature | PostgreSQL | MySQL 8+ | Oracle | SQL Server |
|---------|-----------|----------|--------|------------|
| BOOLEAN | ✓ | ✓ | Use NUMBER(1) | Use BIT |
| TIMESTAMP | ✓ | ✓ | Use TO_TIMESTAMP | ✓ |
| SMALLINT | ✓ | ✓ | ✓ | ✓ |
| DECIMAL | ✓ | ✓ | Use NUMBER | ✓ |

## Key domain concepts

- **Spot rate**: Exchange rate for immediate settlement (T+2 for most pairs, T+1 for USD/CAD)
- **Bid/Ask/Mid**: Bid = rate market buys at; Ask = rate market sells at; Mid = midpoint
- **Forward points**: Pips added to spot rate to get forward rate; reflect interest rate differentials
- **WMR Fix**: WM/Reuters 4pm London benchmark rate used for fund valuations and performance measurement
- **ECB Fix**: European Central Bank reference rates published at ~11:15 CET — authoritative for EUR crosses
- **BFIX**: Bloomberg's FX benchmark published at 15 fixing windows throughout the trading day
- **Pip**: Smallest price movement — 0.0001 for most pairs, 0.01 for JPY pairs

## Useful queries

```sql
-- 1. Current spot rates with pair and provider info
SELECT cp.pair_code, rp.provider_code, er.bid_rate, er.ask_rate, er.mid_rate,
       ROUND(er.ask_rate - er.bid_rate, 6) AS spread,
       er.rate_timestamp
FROM exchange_rate er
JOIN currency_pair cp ON er.pair_id = cp.pair_id
JOIN rate_provider rp ON er.provider_id = rp.provider_id
WHERE er.is_valid = TRUE
ORDER BY er.rate_timestamp DESC;

-- 2. Compare intraday mid vs prior EOD fixing
SELECT cp.pair_code,
       er.mid_rate    AS intraday_mid,
       ef.fixing_rate AS prev_eod_fix,
       ROUND((er.mid_rate - ef.fixing_rate) / ef.fixing_rate * 100, 4) AS pct_change
FROM exchange_rate er
JOIN eod_fixing ef  ON er.pair_id = ef.pair_id AND ef.fixing_date = '2026-03-25'
JOIN currency_pair cp ON er.pair_id = cp.pair_id
WHERE er.is_valid = TRUE
  AND ef.fixing_type = 'WMR'
ORDER BY ABS(er.mid_rate - ef.fixing_rate) / ef.fixing_rate DESC;

-- 3. Open alerts by severity
SELECT ra.severity, cp.pair_code, ra.alert_type, ra.alert_message, ra.triggered_at
FROM rate_alert ra
JOIN currency_pair cp ON ra.pair_id = cp.pair_id
WHERE ra.status = 'OPEN'
ORDER BY CASE ra.severity WHEN 'CRITICAL' THEN 1 WHEN 'WARNING' THEN 2 ELSE 3 END,
         ra.triggered_at;
```
