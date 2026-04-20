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

All scripts auto-detect whether to connect via Docker or local MySQL.
On Windows (Git Bash) they use the default MySQL 8.0 install path.
On Linux they use `docker exec` if the container is running, otherwise fall back to local `mysql`/`mysqldump`.

Common flags available on all scripts:

| Flag | Description | Default |
|------|-------------|---------|
| `-m` | Mode: `auto`, `docker`, `local` | `auto` |
| `-c` | Docker container name | `fx-rate-db` |
| `-h` | MySQL host (local mode) | `localhost` |
| `-P` | MySQL port (local mode) | `3306` |
| `-u` | MySQL user | `root` |
| `-p` | MySQL password | `rootpassword` |

---

### `db_dump.sh`
Creates a full backup — schema, data, routines and triggers — into a `.sql` file.

```bash
# auto detect (docker if running, otherwise local)
./db_dump.sh

# force docker
./db_dump.sh -m docker

# remote host
./db_dump.sh -m local -h linux-poland14.neueda.com

# custom output file
./db_dump.sh -o my_backup.sql
```

---

### `db_reload.sh`
Drops and recreates the database from a dump file. Useful to reset to a known state.

```bash
# auto detect, default dump file
./db_reload.sh

# custom dump file
./db_reload.sh -f my_backup.sql

# force docker
./db_reload.sh -m docker -f my_backup.sql
```

---

### `rebuild_indexes.sh`
Runs `OPTIMIZE TABLE` on all core tables and prints how long it took.
Use after bulk data loads or to keep query performance healthy.

```bash
# auto detect
./rebuild_indexes.sh

# force docker
./rebuild_indexes.sh -m docker
```

---

### `stale_rates_report.sh`
Lists currency pairs not updated within the last N hours.
Shows pair, type, mid rate, last update time and age.

```bash
# default 4 hour threshold
./stale_rates_report.sh

# custom threshold
./stale_rates_report.sh -t 2

# force docker with custom threshold
./stale_rates_report.sh -m docker -t 8
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

- Docker and Docker Compose installed and running
- Ports `8081` (app) and `3306` (MySQL) available

### Start with Docker Compose

Run from repository root:

```bash
docker-compose up --build -d
```

Check status:

```bash
docker-compose ps
```

Tail logs:

```bash
docker-compose logs -f app
docker-compose logs -f db
```

### Verify services

- API base: `http://{LINUX_MACHINE_IP}:8081/api`
- Swagger UI: `http://{LINUX_MACHINE_IP}:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://{LINUX_MACHINE_IP}:8081/v3/api-docs`

Quick endpoint test:

```bash
curl http://{LINUX_MACHINE_IP}:8081/api/currencies
```

Expected status code: `200`

### Stop services

```bash
docker-compose down
```

### Reset database completely (delete MySQL volume)

```bash
docker-compose down -v
docker-compose up --build -d
```

### Notes

- MySQL data persists in named volume `mysql_data`.
- DB init scripts run only when the volume is empty.
- App uses environment-based DB settings from `application.properties` fallbacks.
