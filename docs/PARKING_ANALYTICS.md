# Parking Analytics — Operations & Configuration Guide

This document covers the Smart Parking Analytics module introduced on
`feature/parking-analytics-history`: permanent booking lifecycle history,
booking retention protection, occupancy snapshots, the analytics REST API,
and the Chart.js dashboard (`/analytics.html`).

---

## 1. Prerequisites

| Requirement | Version / Notes |
| :--- | :--- |
| Java | **JDK 21** (`pom.xml` → `java.version=21`) |
| Build tool | Maven 3.8+ (`mvn`; no wrapper committed — install Maven separately) |
| Framework | Spring Boot **3.2.0** (`spring-boot-starter-parent`), Spring Data JPA / Hibernate 6.x |
| Database | PostgreSQL **12+** server (developed against 16/18); JDBC driver version comes from the Spring Boot BOM |
| Reporting time zone | Asia/Kolkata (see §7) |
| Dashboard charts | Chart.js **4.4.1** via CDN (see §8) |
| Frontend | Static HTML5/CSS3/vanilla JS — no build step, no npm |

### Verified component versions

| Component | Version | Source of truth |
| :--- | :--- | :--- |
| Java | 21 | `pom.xml` → `java.version` |
| Spring Boot | 3.2.0 | `pom.xml` parent |
| PostgreSQL JDBC | Spring Boot BOM-managed | `pom.xml` → `org.postgresql:postgresql` (runtime) |
| Flyway | Spring Boot BOM-managed (`flyway-core` 9.x line) | `pom.xml` parent |
| Chart.js | 4.4.1 | `analytics.html` CDN URL |
| Font Awesome | 6.4.0 | `analytics.html` CDN URL |
| Maven | 3.8+ required; 3.9.x used for development | local toolchain |

## 2. Database setup

1. Create the database (example):

   ```bash
   createdb smart_parking_db
   # or
   psql -U postgres -c "CREATE DATABASE smart_parking_db;"
   ```

2. Flyway creates and versions the schema automatically on first startup
   (`spring.flyway.enabled=true`, `baseline-on-migrate=true`,
   `validate-on-migrate=true`, `clean-disabled=true`,
   `out-of-order=false`). **Do not edit or reorder existing migrations.**

### Migration sequence V1–V7

| Version | File | Purpose |
| :--- | :--- | :--- |
| V1 | `V1__Synchronize_Schema.sql` | `parking_slots`, `bookings` tables, indexes |
| V2 | `V2__Fix_Active_Vehicle_Constraint.sql` | Partial unique index: one active booking per vehicle |
| V3 | `V3__Create_Staff_Users.sql` | `staff_users` table + default `admin` account |
| V4 | `V4__Fix_Admin_Password.sql` | Re-seeds admin BCrypt password (`Admin@123`) |
| V5 | `V5__Booking_Events.sql` | `booking_events` table + `duration_minutes` backfill |
| V6 | `V6__Booking_Events_Integrity.sql` | Idempotent-event unique index; `ON DELETE RESTRICT` retention guard |
| V7 | `V7__Occupancy_Snapshots.sql` | `occupancy_snapshots` table with `(floor, captured_at)` uniqueness |

V5–V7 are additive only: no historical migration is modified, existing
`bookings` rows are preserved, and V5 backfills `duration_minutes` from
`exit_time - booking_time` for legacy rows.

### Verifying migration success

After startup, confirm Flyway applied V1–V7:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

All rows should show `success = true` and the latest version should be
`7`. The app also logs `Successfully applied N migrations` at startup.
If `validate-on-migrate` fails (checksum mismatch), do **not** edit the
migration files — resolve via `flyway repair` only after confirming the
database is disposable, or point `DB_URL` at a clean database.

### Connecting to a clean database

Point the datasource at any empty PostgreSQL database; Flyway creates
the full schema on first boot:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/my_new_db
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
mvn spring-boot:run
```

Note the profile difference: `dev` uses `spring.jpa.hibernate.ddl-auto=update`
while `prod` uses `validate` — Flyway is the schema authority in both.

## 3. Environment variables

Profile-specific properties resolve these variables (defaults shown for dev):

| Variable | Purpose | Dev default |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` | `dev` |
| `SERVER_PORT` | HTTP port | `8081` |
| `ACTUATOR_PORT` | Actuator port | `8082` |
| `SSL_ENABLED` | Enable HTTPS | `false` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/smart_parking_db` |
| `DB_USERNAME` | DB user | `postgres` |
| `DB_PASSWORD` | DB password | *(see `.env.example`)* |
| `PARKING_API_KEY` | API key (prod) | required in prod |
| `SSL_KEYSTORE_PATH` / `SSL_KEY_ALIAS` / `SSL_KEYSTORE_PASSWORD` | TLS keystore (prod) | — |
| `CORS_ALLOWED_ORIGINS` | CORS allow-list (prod) | `*` |

Copy `.env.example` to `.env` and fill in real values. `.env` is
git-ignored — never commit credentials.

The `parking.analytics.*` properties also bind from environment
variables via Spring relaxed binding (no code change needed):

| Property | Env var equivalent | Default |
| :--- | :--- | :--- |
| `parking.analytics.snapshot-enabled` | `PARKING_ANALYTICS_SNAPSHOT_ENABLED` | `true` |
| `parking.analytics.snapshot-interval-ms` | `PARKING_ANALYTICS_SNAPSHOT_INTERVAL_MS` | `900000` |
| `parking.analytics.snapshot-initial-delay-ms` | `PARKING_ANALYTICS_SNAPSHOT_INITIAL_DELAY_MS` | `60000` |
| `parking.analytics.zone-id` | `PARKING_ANALYTICS_ZONE_ID` | `Asia/Kolkata` |

## 4. Application startup

```bash
# Option A — local Maven (dev profile)
mvn spring-boot:run

# Option B — packaged jar
mvn clean package
java -jar target/smart-parking-*.jar

# Option C — Docker
cp .env.example .env   # then edit
docker compose up --build -d
```

The app listens on `http://localhost:8081` (dev). Actuator health is on
port `8082` (`/actuator/health`).

**Recommended JVM timezone**: booking timestamps are stored as
`LocalDateTime` in the JVM default zone, so run the app in IST for
correct hourly buckets and date filtering:

```bash
java -Duser.timezone=Asia/Kolkata -jar target/smart-parking-*.jar
# or for spring-boot:run:
MAVEN_OPTS="-Duser.timezone=Asia/Kolkata" mvn spring-boot:run
```

Then open `http://localhost:8081/analytics.html` in a browser (log in
with the seeded staff account first — the dashboard requires a session).

### Login

Staff authentication is session-based (`POST /api/auth/login`). The
migration-seeded account is `admin` / `Admin@123` — **change it before any
real deployment**. Analytics pages require an authenticated session;
unauthenticated `/api/analytics/**` calls return `401`.

## 5. Analytics API inventory

All endpoints are `GET`, session-protected via `AuthInterceptor`
(`/api/analytics/**`), return `Cache-Control: no-store`, and accept
`from`/`to` ISO dates (`yyyy-MM-dd`). When omitted, analytics endpoints
default to the **last 30 days**; `to` is treated as inclusive
(implementation uses `[from, to+1day)`).

| Endpoint | Description |
| :--- | :--- |
| `/api/analytics/overview` | KPI overview: completed sessions, recorded revenue, averages, live occupancy, previous-period comparison |
| `/api/analytics/revenue` | Recorded revenue by day/week/month, payment method, floor; peak day |
| `/api/analytics/occupancy` | Live occupancy + historical snapshots trend, per-floor utilization, peak periods; optional `floor` param scopes **all** occupancy metrics (trend, averages, per-floor breakdown, peaks and live current slots) |
| `/api/analytics/occupancy/health` | Snapshot collection health: enabled flag, configured interval/zone, latest & earliest snapshot, last-24h row count, overdue flag, status (`ACTIVE`/`DELAYED`/`NO_SNAPSHOTS`/`DISABLED`) and missing-interval detection |
| `/api/analytics/vehicles` | Completed sessions by vehicle type, floor mix, daily entry trend |
| `/api/analytics/operations` | Booking volumes, status mix, payment-method counts, daily trends, lifecycle event count |
| `/api/analytics/hourly-traffic` | Entries/exits/revenue per hour of day + top peak hours + reporting timezone |
| `/api/analytics/booking-history` | Paged permanent booking history (PII masked); filters: `status`, `vehicleType`, `paymentMethod`, `active`, `page`, `size` (max 200) |
| `/api/analytics/bookings/{id}/history` | Ordered lifecycle events for one booking |
| `/api/analytics/reports/transactions.csv` | CSV export of booking history (PII masked, formula-injection safe, capped at 50,000 rows); accepts the same `vehicleType`/`paymentMethod` filters as `booking-history` in addition to `from`/`to` |

Notes:

- "Recorded revenue" = `parking_fee` on bookings with a recorded
  `payment_method` **and** `payment_time`. It is not verified settlement.
  Unpaid exits still count as completed sessions (count, duration, status,
  vehicle-type session counts) but contribute no revenue.
- `/api/analytics/vehicles` `avgRevenue` is the average `parking_fee` over
  **all** completed sessions of that vehicle type — including unpaid exits.
  It measures average assessed fee per session, not recorded (collected)
  revenue; use `/api/analytics/revenue` for payment-filtered figures.
- Previous-period comparisons use an equal-length window immediately
  before `from`.
- `booking-history` defaults to **all history** when `from`/`to` are
  omitted; the CSV export defaults to the last 30 days (same as the other
  analytics endpoints).
- Booking-history PII (vehicle number, customer name, phone) is masked
  (`***` + last 3 / first 2 + `***` / `***-***-` + last 4), matching
  `DataSyncService`. Event-history `additionalDetails.vehicleNumber` is
  masked at serialization time; stored audit rows keep full data.
- CSV cells are neutralized against spreadsheet formula injection
  (`=`, `+`, `-`, `@`, tab, CR, LF prefixed with `'`).

### Error responses

| Status | When | Body |
| :--- | :--- | :--- |
| `400` | Malformed date/enum/int params, `from > to`, `floor < 1` | `{"success":false,"error":"..."}` |
| `401` | No/expired session on any `/api/analytics/**` call | `{"success":false,"message":"Unauthorized access. Please login."}` |
| `404` | Unknown booking ID on `/bookings/{id}/history` | `{"success":false,"error":"Booking not found",...}` |
| `500` | Unexpected server failure | `{"success":false,"error":"Failed to ..."}` — never includes stack traces or SQL details |

All analytics responses (including errors and the CSV download) carry
`Cache-Control: no-store` + `Pragma: no-cache`.

## 6. Occupancy snapshots — operations

| Property | Default | Meaning |
| :--- | :--- | :--- |
| `parking.analytics.snapshot-enabled` | `true` | Master switch for collection |
| `parking.analytics.snapshot-interval-ms` | `900000` (15 min) | Fixed delay between collections |
| `parking.analytics.snapshot-initial-delay-ms` | `60000` | Delay before first scheduled run |
| `parking.analytics.zone-id` | `Asia/Kolkata` | Zone used to quantize snapshot buckets |

Behaviour:

- **Initial snapshot**: `OccupancySnapshotInitializer` (an
  `ApplicationRunner`) captures a snapshot as soon as the context is
  ready; the scheduler then runs every interval.
- **Bucketing**: `captured_at` is quantized to the interval boundary on
  the epoch timeline in `parking.analytics.zone-id`, so restarts land in
  the same bucket.
- **Multi-instance deduplication**: collection first tries a
  transaction-scoped PostgreSQL advisory lock
  (`pg_try_advisory_xact_lock`); the `(floor, captured_at)` unique
  constraint is a second dedupe layer. Only one row per floor per bucket
  can exist. Each floor row is persisted in its own `REQUIRES_NEW`
  transaction (`OccupancySnapshotWriter`), so a duplicate or failed
  insert for one floor cannot roll back sibling floors — on PostgreSQL
  a constraint violation aborts the whole transaction, so catching it
  inside a shared transaction cannot work.
- **Health & missing intervals**: `GET /api/analytics/occupancy/health`
  reports collection status from real snapshot rows. Expected buckets
  are computed per floor on the same epoch-quantized grid used at
  capture time, between each floor's first and last observed snapshot —
  the period before the first-ever snapshot is never counted as missing,
  and bucket math (not wall-clock age) absorbs normal scheduler drift.
  `overdue` is true when the newest snapshot is at least one full
  expected interval behind the current bucket; the dashboard surfaces
  this as a warning strip in the Parking Utilization section.
- **Historical tracking limitation**: snapshots only exist from the
  moment collection was enabled. Earlier periods have **no** occupancy
  history and the dashboard reports `trackingStart` accordingly — no
  history is fabricated or backfilled.
- **Disable collection**: set `parking.analytics.snapshot-enabled=false`
  (env `PARKING_ANALYTICS_SNAPSHOT_ENABLED=false` works via relaxed
  binding). Existing rows are retained; live occupancy KPIs are
  unaffected. The health endpoint then reports `status: DISABLED`.
- **Retention**: there is no automatic purge — rows accumulate at
  roughly `floors × (1440/interval-minutes)` per day (≈192 rows/day for
  2 floors @ 15 min). For long-running deployments, prune old data
  manually, e.g.
  `DELETE FROM occupancy_snapshots WHERE captured_at < now() - interval '90 days';`
  (run during maintenance; the table is append-only so deletes are safe).
- **Health endpoint**: `GET /api/analytics/occupancy/health` reports
  `collectionEnabled`, `snapshotIntervalMs`, `zoneId`,
  `earliestSnapshotAt`, `latestSnapshotAt`, `trackedFloorCount`,
  `snapshotsLast24h`, `overdue`, `status`
  (`ACTIVE`/`DELAYED`/`NO_SNAPSHOTS`/`DISABLED`), and missing-interval
  counts — all computed from real rows, nothing fabricated.

## 7. Time zones

- All `bookings` timestamps are `LocalDateTime` written with the JVM
  default zone. Run the JVM with `-Duser.timezone=Asia/Kolkata` (or on a
  host already in IST) so date filters and hourly buckets match Indian
  business time.
- Analytics date boundaries (`from`/`to`) use `LocalDate` → start/end of
  day in the JVM zone; hourly buckets come from `EXTRACT(HOUR ...)` on
  stored local timestamps. When `from`/`to` are omitted, the default
  window (last 30 days) is computed in `parking.analytics.zone-id`
  (default `Asia/Kolkata`), and `/hourly-traffic` reports that same
  configured zone as `timezone`.
- Snapshot bucketing uses `parking.analytics.zone-id`
  (default `Asia/Kolkata`) independently of the JVM zone — keep the two
  consistent.
- The dashboard computes IST calendar dates in the browser and formats
  server timestamps literally (no browser-TZ conversion).

## 8. Dashboard (`/analytics.html`)

- KPI cards, revenue/occupancy/vehicle/operations/hourly-traffic
  Chart.js sections, transaction table, CSV export button.
- The Parking Utilization section shows an **Occupancy Tracking
  Health** strip (status badge, latest/earliest snapshot, interval,
  24h count, overdue and missing-interval warnings) fed by
  `/api/analytics/occupancy/health`.
- **Chart.js is loaded from CDN** (`cdnjs.cloudflare.com`, v4.4.1). If
  the CDN is unreachable the page still renders all tables/ KPIs; each
  chart box shows "Chart library unavailable" instead of breaking.
  Font Awesome 6.4.0 icons come from the same CDN. Requires a modern
  browser with JavaScript enabled (ES2020+); no build step or npm
  packages are needed — `analytics.html` is a self-contained static
  page served by Spring Boot.
- Filter scope (also shown on the page): date range applies to every
  section; **Floor** applies to Parking Utilization only — every metric
  in that section (trend, averages, per-floor breakdown, peak periods and
  the live "Current slots" doughnut) is scoped to the selected floor;
  **Vehicle type** and **Payment method** apply to the Transaction
  Report table and its CSV export only — they do not affect any chart.
- The transaction table is paginated (page controls + 20/50/100 rows per
  page); filter changes reset to page 1. CSV export honours the date
  range plus the vehicle-type and payment-method filters.

## 9. Running tests

```bash
mvn clean test
```

Tests are JUnit 5 + Mockito unit tests
(`src/test/java/com/smartparking/**`) and do not require a database.
`src/test/resources/application-test.properties` points at a
`smart_parking_test` PostgreSQL database for any context-level tests;
ensure local PostgreSQL is running if such tests are added.

## 10. Security notes

- `/api/analytics/**` requires an authenticated session (same
  `AuthInterceptor` mechanism as `/api/exit/**`); unauthenticated calls
  get `401 {"success":false,...}`.
- All analytics API responses carry `Cache-Control: no-store` and
  `Pragma: no-cache`, set in `AuthInterceptor.preHandle` — so the headers
  are present on successful, error and CSV responses, including
  unauthenticated `401`s. Static assets are unaffected.
- Input validation: `from`/`to` must satisfy `from <= to` (else `400`),
  `floor` must be a positive integer, `page`/`size` are clamped
  (`size` max 200), enums (`status`, `vehicleType`, `BookingStatus`)
  reject unknown values with `400` via Spring conversion, and unknown
  booking IDs return `404`. Error bodies contain only a generic
  `{"success":false,"error":...}` message — no stack traces or SQL
  details.
- PII is masked in `booking-history`, CSV export and event-history
  details (see §5).
- No role restrictions are enforced on analytics endpoints — any
  authenticated staff session can read them, matching the existing
  authorization model of the application.
- Application logs record exception messages and booking IDs only; PII
  fields are not logged by the analytics paths.
