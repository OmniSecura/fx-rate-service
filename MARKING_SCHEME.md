# HSBC Graduate Software Engineering Programme 2026
## Project 03: FX Rate Service — Marking Scheme

**Total Marks: 100** | **Duration: 3 Sprints** | **Max Pages: 2**

---

## Overview

This marking scheme assesses the FX Rate Service project across three development sprints, with emphasis on correctness, code quality, domain understanding, and deployment maturity. The FX domain requires precise handling of bid/ask spreads, stale rate detection, audit trails, and forward point calculations.

---

## Sprint 1: Foundations (25%)

### 1.1 SQL Schema Design (8%)

| Criterion | Excellent (8) | Good (6–7) | Satisfactory (4–5) | Needs Improvement (0–3) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Schema Correctness** | Normalized design with appropriate keys; all 8 entities (currency, currency_pair, rate_provider, exchange_rate, eod_fixing, forward_rate, rate_alert, rate_audit_log) correctly modeled with proper relationships. Foreign keys enforce referential integrity. | Schema is normalized with minor redundancy; most entities present with correct relationships; one minor foreign key issue. | Schema has 1–2 normalization issues; most entities present; foreign key relationships incomplete or loose. | Schema lacks normalization or is missing entities; relationships are undefined or incorrect. |
| **Domain Constraints** | Bid/ask spread constraints enforced (bid ≤ mid ≤ ask); pip size validation for relevant currencies; stale rate flags and timestamps; forward points sign/direction enforced. Check constraints or triggers ensure data integrity. | Most constraints present; bid/ask relationship validated; forward point direction mostly enforced; one constraint missing. | Some constraints present; bid/ask validation incomplete; forward point or pip size handling unclear. | No constraint validation; data integrity relies entirely on application logic. |

### 1.2 Data Loading & Shell Scripts (7%)

| Criterion | Excellent (7) | Good (5–6) | Satisfactory (3–4) | Needs Improvement (0–2) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Data Integrity** | Scripts correctly populate all 8 entities with valid test data; bid/ask spreads are logical; forward rates reflect realistic forward points; eod_fixing and rate_alert entries are consistent. No orphaned records. | Test data populated correctly for all entities; minor inconsistencies in spreads or forward points; referential integrity maintained. | Data populated for most entities; some inconsistencies in bid/ask or forward rates; minor orphaned records. | Incomplete or corrupted data; referential integrity violated; many inconsistencies. |
| **Script Quality** | Shell scripts are idempotent, error-handled, and well-commented; use appropriate SQL syntax; clear separation of schema creation, data insertion, and validation steps. | Scripts functional and mostly error-handled; good structure; minor commenting gaps or syntax inefficiencies. | Scripts functional but lack error handling or comments; syntax could be cleaner. | Scripts fail or are incomplete; no error handling or documentation. |

### 1.3 SQL Queries (10%)

| Criterion | Excellent (10) | Good (7–9) | Satisfactory (5–6) | Needs Improvement (0–4) |
|-----------|----------------|---------|--------------------|------------------------|
| **Query Correctness** | All queries return accurate results; correctly identifies stale rates (using timestamp/validity logic), calculates bid/ask spreads, aggregates forward rates, and produces rate audit trails. Queries handle NULL values and edge cases. | Queries mostly correct; minor logic errors in stale rate detection or spread calculation; audit trail queries mostly complete; one query has edge case issues. | Queries produce mostly correct results; stale rate or audit trail logic incomplete; some inefficient but functional queries. | Queries contain significant logic errors; stale rate detection or spread calculation is broken; results unreliable. |
| **Query Efficiency** | Appropriate indexing strategy implied; queries use JOIN, GROUP BY, and WHERE clauses efficiently; no N+1 patterns. Subqueries or window functions used where beneficial. | Queries are functional; most use appropriate joins and filtering; one or two inefficient patterns. | Queries work but show inefficiencies (e.g., missing WHERE clause, unnecessary Cartesian products); no apparent indexing consideration. | Queries are slow or poorly structured; performance issues evident. |

---

## Sprint 2: Java Service Layer (30%)

### 2.1 JDBC & Data Access (10%)

| Criterion | Excellent (10) | Good (7–9) | Satisfactory (5–6) | Needs Improvement (0–4) |
|-----------|----------------|---------|--------------------|------------------------|
| **JDBC Implementation** | All CRUD operations implemented cleanly; prepared statements used throughout to prevent SQL injection; connection pooling configured; transactions properly managed. DAO/Repository pattern applied consistently. | JDBC operations functional; prepared statements used consistently; basic connection management; transaction handling mostly correct; minor pattern inconsistencies. | JDBC operations work; some use of prepared statements; connection handling basic; transaction support present but incomplete. | SQL injection vulnerabilities; improper connection handling; transaction management missing or broken. |
| **FX Domain Logic** | Bid/ask spread validation in data access layer; stale rate detection logic integrated; forward rate calculations (points applied correctly); rate_audit_log entries created on modifications. | Most domain logic present; spread and stale rate handling correct; minor gaps in forward point calculation or audit trail logging. | Domain logic partially implemented; spread or stale rate detection incomplete; audit trail incomplete. | Little or no domain logic in data access; business rules violated at persistence layer. |

### 2.2 Service Layer Design (8%)

| Criterion | Excellent (8) | Good (6–7) | Satisfactory (4–5) | Needs Improvement (0–3) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Separation of Concerns** | Clear service interfaces; business logic cleanly separated from data access; FX-specific services (e.g., RateCalculationService, AlertService) encapsulate domain logic. Dependencies injected. | Services mostly well-separated; business logic distinct from DAO; one or two layering issues; dependencies managed. | Layering present but blurred; some business logic in DAO or controllers; minor mixing of concerns. | Poor separation; business logic scattered; tight coupling evident. |
| **Domain Services** | Dedicated services for rate spike detection, forward point calculation, stale rate validation, and alert generation. Error handling and edge cases considered. | Most domain services present; core logic correct; one service incomplete or underdeveloped. | Some domain services present; logic mostly correct but one service missing (e.g., alert service or spike detection). | Few or no specialized FX services; domain complexity handled generically. |

### 2.3 Unit Testing (8%)

| Criterion | Excellent (8) | Good (6–7) | Satisfactory (4–5) | Needs Improvement (0–3) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Test Coverage** | ≥80% line coverage; all service methods tested; edge cases covered (e.g., bid > ask, zero forward points, stale rates). Bid/ask spread validation, forward rate calculations, and rate spike invalidation logic all tested. | 60–79% coverage; most service methods tested; most edge cases covered; one domain scenario (e.g., alert logic) under-tested. | 40–59% coverage; key service methods tested; edge cases partially covered; some domain logic untested. | <40% coverage; minimal tests; critical logic untested. |
| **Test Quality** | Mocks (Mockito) used appropriately; assertions are specific and meaningful; tests are independent and fast. Test names clearly describe scenarios. Parametrized tests used where beneficial (e.g., multiple rate scenarios). | Tests use mocks appropriately; assertions mostly clear; tests mostly independent; minor naming or structural issues. | Tests present but some lack clarity; mocking incomplete in places; test interdependencies exist. | Tests are brittle, slow, or rely on external state; unclear assertions. |

---

## Sprint 3: Deployment & REST API (30%)

### 3.1 Spring Boot REST API (10%)

| Criterion | Excellent (10) | Good (7–9) | Satisfactory (5–6) | Needs Improvement (0–4) |
|-----------|----------------|---------|--------------------|------------------------|
| **API Design** | REST endpoints follow conventions; all CRUD operations and FX-specific queries (e.g., `/rates/stale`, `/alerts/active`, `/rates/{pair}/forward`) are implemented. Request/response DTOs properly structured. Status codes are semantically correct. | Endpoints functional and mostly RESTful; main CRUD and query endpoints present; minor naming or design inconsistencies; DTOs mostly well-structured. | Endpoints functional but some non-RESTful patterns; missing one or two domain-specific endpoints; DTO structure adequate but could be cleaner. | Endpoints are inconsistent or non-RESTful; missing key endpoints; poor DTO design. |
| **Error Handling & Validation** | Comprehensive exception handling; meaningful error messages (e.g., "Bid/ask spread invalid" or "Rate stale"); input validation on all endpoints; proper HTTP status codes (400, 404, 500). | Good error handling; most endpoints validate input; error messages mostly clear; minor status code inconsistencies. | Basic error handling; some validation missing; error messages generic or unclear. | Minimal error handling; no input validation; misleading or absent error messages. |

### 3.2 Docker & docker-compose (10%)

| Criterion | Excellent (10) | Good (7–9) | Satisfactory (5–6) | Needs Improvement (0–4) |
|-----------|----------------|---------|--------------------|------------------------|
| **Dockerfile** | Optimized multi-stage build; minimal image size; uses appropriate base image (e.g., openjdk:11-jre-slim); health check configured; non-root user employed. Clear and maintainable. | Standard Dockerfile; image builds successfully; base image appropriate; non-root user or health check missing; minor optimization opportunities. | Dockerfile functional; base image choice adequate; lacks optimization or health check. | Dockerfile has issues (e.g., security risk, inefficient layering); build fails or image is oversized. |
| **docker-compose Configuration** | All services (DB, application) orchestrated cleanly; environment variables managed via .env or compose file; volumes persist data; networks isolate services; startup order correct (depends_on or health checks). | Services orchestrated; environment config present; volumes and networks mostly correct; minor startup order issues. | Services present; basic orchestration works; environment handling functional but could be cleaner; volumes or networking incomplete. | Services not properly orchestrated; environment not managed; missing volumes or networks; startup fails. |

### 3.3 CI/CD & Jenkinsfile (10%)

| Criterion | Excellent (10) | Good (7–9) | Satisfactory (5–6) | Needs Improvement (0–4) |
|-----------|----------------|---------|--------------------|------------------------|
| **Jenkinsfile Structure** | Pipeline clearly stages build, test, Docker build, and deploy steps. Conditional logic for branches (main vs. feature). Artifact archiving and test report aggregation configured. Failure notifications set. | Pipeline includes build, test, and Docker stages; mostly clear; one stage missing logic (e.g., deploy or notifications); minor parameter issues. | Pipeline includes main stages; functional but lacks sophistication; one key stage incomplete or overly simplified. | Pipeline incomplete (missing test or deploy stage); logic unclear; fails to execute properly. |
| **Deployment & Integration** | Deployment to environment (dev/staging/prod) automated; variables and secrets managed securely (not hardcoded); rollback capability or blue-green strategy considered. Integration tests run post-deployment. | Deployment automated; most secrets managed securely; one minor hardcoding; integration tests mostly present. | Deployment partially automated; secret management basic; integration tests missing or minimal. | Deployment manual or non-functional; secrets exposed; no integration testing. |

---

## Code Quality & Collaboration (15%)

### 4.1 Code Quality (8%)

| Criterion | Excellent (8) | Good (6–7) | Satisfactory (4–5) | Needs Improvement (0–3) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Code Standards** | Consistent naming (camelCase, descriptive); small, focused methods; DRY principle applied; few code smells detected. Comments and Javadoc present for complex FX logic (e.g., forward point calculation). | Code mostly clean; naming good; methods mostly focused; minor repetition; Javadoc mostly present. | Code functional; naming adequate; some large methods; moderate repetition; documentation sparse. | Inconsistent style; poor naming; large methods; significant duplication; no documentation. |
| **FX Domain Knowledge** | Code clearly reflects domain understanding: bid/ask relationships enforced, forward points correctly applied, stale rate concept integrated, pip sizes used appropriately, audit trail maintained. Variable and method names use FX terminology. | Domain concepts well-implemented; minor terminology inconsistencies; logic is sound. | Domain logic present; some terminology confusion; edge cases partially handled. | Domain concepts misunderstood or oversimplified; terminology incorrect. |

### 4.2 Git & Collaboration (7%)

| Criterion | Excellent (7) | Good (5–6) | Satisfactory (3–4) | Needs Improvement (0–2) |
|-----------|---------------|-----------|-------------------|------------------------|
| **Git Workflow** | Meaningful commit messages (sprint and feature tags); regular commits; feature branches for each sprint/user story; pull requests with clear descriptions. No merge conflicts left unresolved. | Commits mostly meaningful; branches used for features; PRs present with descriptions; occasional vague messages. | Commits present; some meaningful messages; branching inconsistent; PRs minimal or incomplete. | Large infrequent commits; no branch strategy; commits lack context. |
| **Collaboration & Documentation** | README and sprint documentation clear; contributor guidelines present; code review comments addressed; team responsibilities evident from commit history. | Documentation adequate; most team contributions visible; minor gaps in guidelines. | Documentation present but basic; team roles somewhat unclear. | Little to no documentation; team coordination unclear. |

---

## Bonus Marks (up to 5%)

| Criterion | Marks | Notes |
|-----------|-------|-------|
| **Advanced Features** | +3 | Real-time rate spike alerts, machine learning-based anomaly detection, or advanced rate analytics (correlation, volatility). |
| **Performance Optimization** | +2 | Redis caching for frequently queried rates, batch processing for large rate uploads, or query optimization reducing response time >50%. |
| **Enhanced Observability** | +2 | Structured logging, metrics (Prometheus), distributed tracing (Jaeger), or custom health check endpoints beyond Spring defaults. |
| **Security Hardening** | +2 | Rate limiting, API authentication/authorization (JWT, OAuth), encrypted audit logs, or data masking in error responses. |
| **Comprehensive Documentation** | +1 | API documentation (Swagger/OpenAPI), architecture decision records, FX domain glossary, or deployment runbook. |

**Maximum project score: 105 marks** (100 base + 5 bonus)

---

## Grading Summary

| Score Range | Grade | Interpretation |
|-------------|-------|-----------------|
| 90–105 | A* | Exceptional: All sprints complete, high code quality, strong domain mastery, full deployment. |
| 80–89 | A | Excellent: All sprints complete, good code quality, solid domain understanding, deployment functional. |
| 70–79 | B | Good: All sprints largely complete, adequate code quality, domain logic sound, deployment partial or needs tuning. |
| 60–69 | C | Satisfactory: Most sprints complete, acceptable code quality, domain logic present but incomplete, basic deployment. |
| 50–59 | D | Needs Improvement: Significant gaps in one or more sprints, code quality issues, domain logic incomplete, deployment not achieved. |
| <50 | F | Fail: Major gaps across sprints, poor code quality, domain logic missing, deployment absent. |

---

**Assessor Notes:**
- Deduct marks for late submission (typically 5% per day).
- Consider awarding partial credit in borderline cases; justify in feedback.
- Emphasize the importance of the FX domain: understanding bid/ask spreads, stale rate detection, and audit trails is non-negotiable for a financial services company.
- Document feedback for each sprint separately to guide improvement in subsequent sprints.
