# FX Rate Service — Database

## Views & Stored Procedures

### Views

#### `latest_rates_vw`
Returns the most recent valid spot rate for every active currency pair, with a staleness flag if the rate has not been updated in over 4 hours.

| Column | Description |
|--------|-------------|
| `pair_code` | Currency pair code e.g. `EUR/USD` |
| `pair_type` | MAJOR / MINOR / EXOTIC / CROSS |
| `mid_rate` | Latest mid rate |
| `rate_timestamp` | When the rate was last updated |
| `hours_since_update` | How many hours ago the rate was captured |
| `is_stale` | `1` if older than 4 hours, `0` if fresh |

```sql
SELECT * FROM latest_rates_vw;
SELECT * FROM latest_rates_vw WHERE is_stale = 1;
```

---

#### `rate_history_vw`
Full rate history for all pairs, joined with pair and provider details, ordered newest first. Use this for audit queries and historical analysis.

| Column | Description |
|--------|-------------|
| `pair_code` | Currency pair code |
| `provider_code` | Source provider e.g. `REUTERS` |
| `bid_rate` / `ask_rate` / `mid_rate` | Rate components |
| `spread` | Calculated as `ask - bid` |
| `rate_timestamp` | When the rate was captured |
| `is_valid` | Whether the rate is currently active |

```sql
SELECT * FROM rate_history_vw WHERE pair_code = 'GBP/USD';
SELECT * FROM rate_history_vw WHERE pair_code = 'EUR/USD' AND DATE(rate_timestamp) = '2026-03-26';
```

---

### Stored Procedures

#### `store_rate`
Inserts a new spot rate for a currency pair and marks all previous valid rates for that pair as stale. Calculates mid rate automatically from bid and ask. Raises an error if bid > ask.

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_rate_id` | INT | Primary key for the new rate |
| `p_pair_id` | INT | Currency pair ID |
| `p_provider_id` | INT | Rate provider ID |
| `p_bid_rate` | DECIMAL(18,6) | Bid rate |
| `p_ask_rate` | DECIMAL(18,6) | Ask rate |
| `p_rate_timestamp` | TIMESTAMP | When the rate was captured |
| `p_source_system` | VARCHAR(30) | e.g. `REUTERS`, `BLOOMBERG` |

```sql
CALL store_rate(21, 1, 1, 1.08290, 1.08310, NOW(), 'REUTERS');
```

---

#### `store_fixing`
Inserts an end-of-day fixing into `eod_fixing`. If the fixing rate deviates from the last traded mid rate by more than the threshold (default 1%), a `WARNING` alert is automatically inserted into `rate_alert`.

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_fixing_id` | INT | Primary key for the fixing |
| `p_pair_id` | INT | Currency pair ID |
| `p_provider_id` | INT | Rate provider ID |
| `p_fixing_date` | DATE | Date of the fixing |
| `p_fixing_rate` | DECIMAL(18,6) | Official fixing rate |
| `p_fixing_time` | VARCHAR(10) | e.g. `16:00 LON`, `11:00 ECB` |
| `p_fixing_type` | VARCHAR(20) | `WMR`, `ECB`, `BFIX`, `INTERNAL` |
| `p_is_official` | BOOLEAN | Whether this is an official benchmark fixing |
| `p_published_at` | TIMESTAMP | When the fixing was published |
| `p_threshold` | DECIMAL(5,4) | Deviation threshold e.g. `0.01` = 1% (NULL = default 1%) |

```sql
-- default 1% threshold
CALL store_fixing(21, 1, 8, '2026-04-08', 1.0756, '16:00 LON', 'WMR', TRUE, NOW(), NULL);

-- custom 0.5% threshold
CALL store_fixing(21, 1, 8, '2026-04-08', 1.0756, '16:00 LON', 'WMR', TRUE, NOW(), 0.005);
```

---

#### `get_rate`
Returns the latest valid spot rate for a given currency pair, with a staleness flag based on a configurable threshold.

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_pair_code` | VARCHAR(7) | Currency pair code e.g. `EUR/USD` |
| `p_stale_minutes` | INT | Staleness threshold in minutes (NULL = default 60) |

```sql
CALL get_rate('EUR/USD', 60);
CALL get_rate('GBP/USD', NULL);
```

---

#### `get_cross_rate`
Derives the implied cross rate between two currencies given two overlapping pairs that share a common currency leg. Returns the cross pair code, calculated rate, source rates, and staleness flags for both legs.

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_pair1` | VARCHAR(7) | First pair e.g. `EUR/USD` |
| `p_pair2` | VARCHAR(7) | Second pair e.g. `USD/PLN` |
| `p_stale_minutes` | INT | Staleness threshold in minutes (NULL = default 60) |

```sql
-- derives EUR/PLN from EUR/USD and USD/PLN
CALL get_cross_rate('EUR/USD', 'USD/PLN', 60);

-- derives EUR/USD from EUR/GBP and GBP/USD
CALL get_cross_rate('EUR/GBP', 'GBP/USD', 60);
```

---

## Schema Design Decisions

The schema enforces data integrity at the database level using `CHECK` constraints, rather than relying solely on application logic. In a regulated financial environment, the database is the last line of defence — bad data that passes application validation can still be rejected at the storage layer. Each constraint below is intentional and maps directly to a business or domain rule.

### `currency`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_minor_units` | `minor_units >= 0 AND minor_units <= 4` | Minor units represent the number of decimal places for a currency (e.g. JPY = 0, USD = 2). A negative value has no meaning; values above 4 do not exist in any ISO 4217 currency and would indicate a data entry error. |
| `chk_region` | `region IN ('EMEA', 'AMER', 'APAC')` | Region is used for routing, reporting, and desk assignment. Free-text would allow `'Europe'`, `'emea'`, or `'EUROPE'` to co-exist silently, breaking any GROUP BY or filter that depends on this field. |

### `rate_provider`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_provider_type` | `provider_type IN ('MARKET_DATA', 'CENTRAL_BANK', 'INTERNAL', 'BROKER')` | Provider type determines how rates from that source are weighted and used (e.g. central bank rates are reference-only, not tradeable). A typo like `'MARKET DATA'` would silently exclude that provider from rate selection logic. |
| `chk_priority` | `priority >= 1` | Priority drives which source is preferred when multiple providers quote the same pair. A value of 0 or below has no defined meaning in this system and would cause unpredictable ordering behaviour. |

### `currency_pair`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_pip_size` | `pip_size > 0` | A pip is the smallest measurable price movement. Zero or negative pip sizes would break spread calculations and P&L attribution downstream. |
| `chk_decimal_places` | `decimal_places >= 0 AND decimal_places <= 6` | Controls display precision. Negative values are nonsensical; values above 6 exceed the precision used by any standard FX instrument and would indicate a data error. |
| `chk_different_currencies` | `base_currency <> quote_currency` | A pair where base and quote are the same currency (e.g. `USD/USD`) would always have a rate of exactly 1.0 and is meaningless. Allowing it would risk corrupting conversion calculations. |
| `chk_pair_type` | `pair_type IN ('MAJOR', 'MINOR', 'EXOTIC', 'CROSS')` | Pair type is used to apply different liquidity assumptions, spread tolerances, and alert thresholds. An unrecognised value would silently fall outside all category-based business rules. |

### `exchange_rate`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_spread` | `bid_rate <= mid_rate AND mid_rate <= ask_rate` | This is the fundamental FX pricing rule: the market always buys at the bid (lower) and sells at the ask (higher), with mid sitting between them. A bid above ask would imply a risk-free arbitrage and indicates corrupt data. |
| `chk_rates_positive` | `bid_rate > 0 AND ask_rate > 0 AND mid_rate > 0` | Exchange rates cannot be zero or negative. A zero rate would cause division-by-zero errors in any cross-rate or P&L calculation; a negative rate is physically impossible for a currency price. |
| `chk_source_system` | `source_system IN ('REUTERS', 'BLOOMBERG', 'ECB_FEED', 'HSBC_INT', 'WMR', 'ICAP')` | Source system is recorded for audit and lineage purposes. Unconstrained free-text would make it impossible to reliably trace where a rate originated, which is a regulatory requirement. |

### `eod_fixing`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_fixing_rate_positive` | `fixing_rate > 0` | EOD fixings are used for fund valuations, performance measurement, and regulatory reporting. A zero or negative fixing rate would silently corrupt P&L calculations across all positions valued at that fix. |
| `chk_fixing_type` | `fixing_type IN ('WMR', 'ECB', 'BFIX', 'INTERNAL')` | Each fixing type has a distinct legal and operational significance. An unrecognised type would be excluded from benchmark reconciliation processes without any visible error. |
| `chk_published_after_date` | `published_at >= fixing_date` | A fixing cannot be published before the date it relates to. This constraint catches timestamp mis-mapping errors at the point of ingestion rather than during downstream reconciliation. |

### `forward_rate`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_forward_rate_positive` | `forward_rate > 0` | Forward rates must be positive. Note: `forward_points` are intentionally unconstrained — they are legitimately negative when the forward currency trades at a discount to spot. |
| `chk_tenor` | `tenor IN ('ON', 'TN', '1W', '1M', '2M', '3M', '6M', '1Y')` | Tenors map to specific settlement date calculation conventions. An unrecognised tenor string would prevent the system from computing the correct value date, creating settlement risk. |
| `chk_value_date_future` | `value_date >= rate_timestamp` | A forward rate's value date must fall after the rate was captured. A value date in the past would indicate a mis-keyed record that could cause incorrect settlement instructions. |

### `rate_alert`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_severity` | `severity IN ('INFO', 'WARNING', 'CRITICAL')` | Severity controls escalation paths — `CRITICAL` alerts may trigger automated desk notifications. Free-text values like `'WARN'` would silently fall outside all escalation rules. |
| `chk_status` | `status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')` | Alert lifecycle management depends on this field. An unrecognised status would cause alerts to disappear from all standard dashboards without being resolved. |
| `chk_alert_type` | `alert_type IN ('THRESHOLD_BREACH', 'STALE_RATE', 'SPREAD_WIDE', 'SPIKE')` | Alert type determines the response playbook. An unrecognised type would bypass all automated handling logic. |
| `chk_ack_after_trigger` | `acknowledged_at IS NULL OR acknowledged_at >= triggered_at` | An acknowledgement timestamp before the alert was triggered is logically impossible and indicates a system clock or timezone error. |

### `rate_audit_log`

| Constraint | Rule | Rationale |
|---|---|---|
| `chk_action` | `action IN ('INSERT', 'UPDATE', 'INVALIDATE')` | The audit log is the authoritative record of all changes to rate data. Constraining the action field ensures the log can be reliably parsed by compliance and reconciliation tooling. |
