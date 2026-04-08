# FX Rate Service — Database Scripts

Shell scripts for basic DBA operations on the `fx_rate_db` MySQL database.

> All scripts require Git Bash on Windows and MySQL Server 8.0 installed at the default path.

---

## Scripts

### `db_dump.sh`
Creates a full backup of the database — schema, data, routines and triggers — into a single `.sql` file saved in the `dumps/` folder with a timestamp in the filename.

```bash
./db_dump.sh
```

---

### `db_reload.sh`
Drops the existing database and recreates it from a dump file. Useful when you want to reset your local database to a known state.

```bash
./db_reload.sh
```

By default it looks for `fx_rate_dump.sql` in the current folder. You can pass a custom dump file:

```bash
./db_reload.sh fx_rate_db fx_rate_dump.sql
```

---

### `rebuild_indexes.sh`
Runs `OPTIMIZE TABLE` on all core tables and prints how long it took. Use this after bulk data loads or to keep query performance healthy.

```bash
./rebuild_indexes.sh
```

---

### `stale_rates_report.sh`
Queries the database and lists all currency pairs that have not been updated in the last N hours. Default threshold is 4 hours. Output shows the pair, type, mid rate, last update time and how long ago that was.

```bash
# default — 4 hour threshold
./stale_rates_report.sh

# custom threshold, e.g. 2 hours
./stale_rates_report.sh 2
```

Example output:
```
pair     type    mid_rate    last_updated          age
EUR/USD  MAJOR   1.083150    2026-03-26 08:00:01   11d 3h 59m
GBP/USD  MAJOR   1.296250    2026-03-26 08:00:02   11d 3h 59m
```

---

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