# fx-rate-service
Foreign Exchange - Rates, History &amp; Conversion 
# Team 3 Final Project 

Graduate Software Engineering - Final Project

## Team Members
- Asia
- Dominika
- Bartosz

# FX Rate Service — Database Scripts

Shell scripts for basic DBA operations on the `fx_rate_db` MySQL database.

> All scripts require Git Bash on Windows and MySQL Server 8.0 installed at the default path.

## Database Setup

1. Create database:
   CREATE DATABASE fx_rate_db;

2. Load schema and data:
   mysql -u root -p fx_rate_db < 00_ddl_schema.sql
   

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

## Docker Setup (App + MySQL)

The project now includes:

- `spring-boot-app/Dockerfile` for the Spring Boot API
- `docker-compose.yml` for running API + MySQL together
- `docker/mysql/init/00-load-schema-data.sh` to initialize schema, seed data, views, and procedures

### Prerequisites

- Docker Desktop installed and running
- Ports `8080` (app) and `3306` (MySQL) available

### Start with Docker Compose

Run from repository root:

```powershell
Set-Location "C:\Users\Administrator\Desktop\FinalProject17.04\fx-rate-service"
docker compose up --build -d
```

Check status:

```powershell
docker compose ps
```

Tail logs:

```powershell
docker compose logs -f app
docker compose logs -f db
```

### Verify services

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Quick endpoint test:

```powershell
Invoke-WebRequest "http://localhost:8080/api/currencies" | Select-Object -ExpandProperty StatusCode
```

Expected status code: `200`

### Stop services

```powershell
docker compose down
```

### Reset database completely (delete MySQL volume)

```powershell
docker compose down -v
docker compose up --build -d
```

### Notes

- MySQL data persists in named volume `mysql_data`.
- DB init scripts run only when the volume is empty.
- App uses environment-based DB settings from `application.properties` fallbacks.
