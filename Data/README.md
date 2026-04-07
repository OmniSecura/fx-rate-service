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

---

## Schema design decisions

The schema enforces data integrity at the database level using `CHECK` constraints, rather than relying solely on application logic. In a regulated financial environment, the database is the last line of defence — bad data that passes application validation can still be rejected at the storage layer. Each constraint below is intentional and maps directly to a business or domain rule.

### `currency`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_minor_units` | `minor_units >= 0 AND minor_units <= 4` | Minor units represent the number of decimal places for a currency (e.g. JPY = 0, USD = 2). A negative value has no meaning; values above 4 do not exist in any ISO 4217 currency and would indicate a data entry error. |
| `chk_region` | `region IN ('EMEA', 'AMER', 'APAC')` | Region is used for routing, reporting, and desk assignment. Free-text would allow `'Europe'`, `'emea'`, or `'EUROPE'` to co-exist silently, breaking any GROUP BY or filter that depends on this field. |

### `rate_provider`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_provider_type` | `provider_type IN ('MARKET_DATA', 'CENTRAL_BANK', 'INTERNAL', 'BROKER')` | Provider type determines how rates from that source are weighted and used (e.g. central bank rates are reference-only, not tradeable). A typo like `'MARKET DATA'` (with a space) would silently exclude that provider from rate selection logic. |
| `chk_priority` | `priority >= 1` | Priority drives which source is preferred when multiple providers quote the same pair. A value of 0 or below has no defined meaning in this system and would cause unpredictable ordering behaviour. |

### `currency_pair`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_pip_size` | `pip_size > 0` | A pip is the smallest measurable price movement. Zero or negative pip sizes would break spread calculations and P&L attribution downstream. |
| `chk_decimal_places` | `decimal_places >= 0 AND decimal_places <= 6` | Controls display precision. Negative values are nonsensical; values above 6 exceed the precision used by any standard FX instrument and would indicate a data error. |
| `chk_different_currencies` | `base_currency <> quote_currency` | A pair where the base and quote are the same currency (e.g. `USD/USD`) would always have a rate of exactly 1.0 and is meaningless. Allowing it would risk corrupting conversion calculations that assume a genuine exchange relationship. |
| `chk_pair_type` | `pair_type IN ('MAJOR', 'MINOR', 'EXOTIC', 'CROSS')` | Pair type is used to apply different liquidity assumptions, spread tolerances, and alert thresholds. An unrecognised value would silently fall outside all category-based business rules. |

### `exchange_rate`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_spread` | `bid_rate <= mid_rate AND mid_rate <= ask_rate` | This is the fundamental FX pricing rule: the market always buys at the bid (lower) and sells at the ask (higher), with mid sitting between them. A bid above ask would imply a risk-free arbitrage and indicates corrupt or mis-mapped data. |
| `chk_rates_positive` | `bid_rate > 0 AND ask_rate > 0 AND mid_rate > 0` | Exchange rates cannot be zero or negative. A zero rate would cause division-by-zero errors in any cross-rate or P&L calculation; a negative rate is physically impossible for a currency price. |
| `chk_source_system` | `source_system IN ('REUTERS', 'BLOOMBERG', 'ECB_FEED', 'HSBC_INT', 'WMR', 'ICAP')` | Source system is recorded for audit and lineage purposes. Unconstrained free-text would make it impossible to reliably trace where a rate originated, which is a regulatory requirement. |

### `eod_fixing`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_fixing_rate_positive` | `fixing_rate > 0` | EOD fixings are used for fund valuations, performance measurement, and regulatory reporting. A zero or negative fixing rate would silently corrupt P&L calculations across all positions valued at that fix. |
| `chk_fixing_type` | `fixing_type IN ('WMR', 'ECB', 'BFIX', 'INTERNAL')` | Each fixing type has a distinct legal and operational significance — WMR is used for fund NAV calculations, ECB for certain EU instruments. An unrecognised type would be excluded from benchmark reconciliation processes without any visible error. |
| `chk_published_after_date` | `published_at >= fixing_date` | A fixing cannot be published before the date it relates to. This constraint catches timestamp mis-mapping errors at the point of ingestion rather than during downstream reconciliation. |

### `forward_rate`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_forward_rate_positive` | `forward_rate > 0` | Like spot rates, forward rates must be positive. A zero or negative value would cause failures in any interest rate differential or hedge ratio calculation. Note: `forward_points` are intentionally unconstrained — they are legitimately negative when the forward currency trades at a discount to spot (e.g. EUR/USD at longer tenors). |
| `chk_tenor` | `tenor IN ('ON', 'TN', '1W', '1M', '2M', '3M', '6M', '1Y')` | Tenors map to specific settlement date calculation conventions. An unrecognised tenor string (e.g. `'1 Month'` or `'30D'`) would prevent the system from computing the correct value date, creating settlement risk. |
| `chk_value_date_future` | `value_date >= rate_timestamp` | A forward rate's value date must fall after the rate was captured. A value date in the past relative to the rate timestamp would indicate a mis-keyed record that could cause incorrect settlement instructions. |

### `rate_alert`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_severity` | `severity IN ('INFO', 'WARNING', 'CRITICAL')` | Severity controls escalation paths — `CRITICAL` alerts may trigger automated desk notifications or position limits. Free-text values like `'WARN'` or `'warning'` would silently fall outside all escalation rules. |
| `chk_status` | `status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')` | Alert lifecycle management depends on this field for filtering and reporting. An unrecognised status would cause alerts to disappear from all standard dashboards without being resolved. |
| `chk_alert_type` | `alert_type IN ('THRESHOLD_BREACH', 'STALE_RATE', 'SPREAD_WIDE', 'SPIKE')` | Alert type determines the response playbook. An unrecognised type would bypass all automated handling logic. |
| `chk_ack_after_trigger` | `acknowledged_at IS NULL OR acknowledged_at >= triggered_at` | An acknowledgement timestamp before the alert was triggered is logically impossible and indicates a system clock or timezone error. This prevents misleading audit trails. |

### `rate_audit_log`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_action` | `action IN ('INSERT', 'UPDATE', 'INVALIDATE')` | The audit log is the authoritative record of all changes to rate data. Constraining the action field ensures the log can be reliably parsed by compliance and reconciliation tooling. An unrecognised action value would break audit trail completeness. |