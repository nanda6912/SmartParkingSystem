# Parking Analytics – Implementation Plan

Branch: `feature/parking-analytics-history`
Scope: repository analysis and implementation planning only. No analytics code is implemented in this task.

---

## 1. Existing Architecture Summary

### Stack
- Java 21, Spring Boot 3.2.0, Spring MVC + Thymeleaf (mostly static HTML), Spring Data JPA, Flyway, PostgreSQL, Caffeine cache, session-based auth (custom, `staff_users` table, BCrypt via `spring-security-crypto`), Zxing QR generation, iText7 (dependency present, lightly used).
- Frontend: static HTML pages (`index.html`, `ground-floor.html`, `floor1.html`, `exit.html`, `login.html`) sharing `static/styles/design-system.css` and a common sidebar nav. No SPA framework.

### Domain model
- `ParkingSlot` (`parking_slots`): `id`, `slotNumber`, `floor` (1–2), `slotId` (e.g. `AG01`, `FF03`), `status` (`AVAILABLE | LOCKED | OCCUPIED`), `lockUntil`, `@Version`. 600 slots across 2 floors.
- `Booking` (`bookings`): `id`, `bookingCode` (5-char unique), `parkingSlot` (FK), `vehicleNumber`, `customerName`, `phoneNumber`, `vehicleType` (`CAR|BIKE|SUV|VAN`), `bookingTime`, `exitTime`, `parkingFee` (Integer ₹), `durationMinutes` (set only at exit in some paths — check; `processExit` does NOT set it, `processExitWithPayment` does NOT either), `status` (`ACTIVE|COMPLETED|CANCELLED`), `isActive` (denormalized mirror of ACTIVE status), `paymentMethod`, `transactionId` (last-5-digit UPI ref only), `paymentTime`.
- `StaffUser` (`staff_users`): `username`, BCrypt `password`, `fullName`, `role` (e.g. `ADMIN`), `enabled`, `createdAt`.

### Booking lifecycle (as implemented)
1. **Lock**: `POST /api/parking-slots/lock/{slotId}` → `ParkingService.lockSlot` — pessimistic lock via `findByIdWithLock` (`PESSIMISTIC_WRITE`), sets `LOCKED` + `lockUntil = now+2min`.
2. **Book (entry)**: `POST /api/parking-slots/book` → `ParkingService.bookSlot` — requires slot still `LOCKED` and lock not expired; blocks duplicate *active* vehicle via `uk_active_vehicle_only` partial unique index (`vehicle_number WHERE is_active = TRUE`); saves `Booking` with `bookingTime = now`, then sets slot `OCCUPIED`. **`bookingTime` is both creation and physical entry time — there is no separate entry timestamp.**
3. **Lock expiry**: `LockReleaseScheduler` every 30 s releases expired `LOCKED` slots (slots only — no booking exists yet, so nothing to record).
4. **Exit (no payment)**: `POST /api/exit/process/{bookingId}` → `ExitService.processExit` — sets `exitTime`, `parkingFee`, `isActive=false`, slot → `AVAILABLE`.
5. **Exit (with payment)**: `POST /api/exit/process-payment/{bookingId}` → `ExitService.processExitWithPayment` — same plus `paymentMethod` (`UPI`/`CASH`), `transactionId` (validated as exactly 5 digits for UPI), `paymentTime`.
6. Fee: `HOURLY_RATE = 20.0` hardcoded in `ExitService`, `DataSyncService`; `hours = ceil(minutes/60)`, `fee = hours*20`, stored as `Integer` in `parking_fee`. `duration_minutes` column exists but is **not written by either exit path** (always NULL/0).

### Sync / caching layer
- `DataSyncService` caches today's exits in Caffeine (`exitData` cache, 8h TTL, plus in-memory fallback) via `@Cacheable`/`@CachePut`/`@CacheEvict`; endpoints under `/api/sync/**` and `/api/exit/sync-data`. PII masking is applied when converting `Booking` → Map.
- `DataSyncController.processExitWithSync` calls `exitService.processExit` — a second exit path duplicating `ExitController.processExit`.

### Security
- `AuthInterceptor` (registered in `WebConfig` for `/api/exit/**`, `/exit.html`) requires `SESSION_USER_KEY` session attribute; writes 401 JSON otherwise.
- All other endpoints — `/api/parking-slots/**`, `/api/sync/**`, `/api/auth/**`, receipt endpoints — are **unauthenticated**. `/api/exit/**` protects *all* exit APIs including receipts and UPI QR generation (session-gated, role not checked).
- `StaffUser.role` exists but is never enforced anywhere.
- CORS: `WebConfig` allows `*` origins with `allowCredentials=false`; controllers also declare `@CrossOrigin(origins = ${app.cors.allowed-origins})`.

### Data access
- `BookingRepository`: `findByIdWithParkingSlot`, `findByBookingCodeWithParkingSlot`, `findActiveBookingsWithoutExit`, `findExitedBookingsFromDate`, `findActiveBookingByVehicleNumber`, `countByIsActiveTrue`, `countCompletedBookingsInPeriod`, `calculateRevenueInPeriod` (the last two are unused by analytics today).
- `ParkingSlotRepository`: `findByIdWithLock`, `findExpiredLockedSlots`, `countByFloorAndStatus`, status queries.

### Migrations
- `V1` creates `parking_slots` + `bookings` (PostgreSQL `BIGSERIAL`), indexes.
- `V2` fixes `uk_active_vehicle_only` partial unique index.
- `V3`/`V4` create `staff_users` and fix admin password.
- Flyway enabled in dev + prod (`baseline-on-migrate`, `validate-on-migrate`). **Dev profile uses `ddl-auto=update`, prod uses `validate`** — new tables should be created via Flyway migrations only; entity additions must be migration-backed.

### Tests / config
- Only 2 test classes: `AuthControllerTest`, `ParkingControllerTest` (WebMvc). No service/repository tests. `application-test.properties` exists.
- `application-dev.properties` default; Docker + CI/CD workflows present.

---

## 2. Data Limitations Discovered (verified against code)

| Question | Finding |
|---|---|
| Booking creation vs vehicle entry separate? | **No.** `bookingTime` is set at booking creation and the slot is immediately `OCCUPIED`; there is no distinct entry timestamp. They are the same instant. |
| How are completed bookings identified? | `exitTime != null` AND `isActive = false` (set together in `processExit`/`processExitWithPayment`). `status` becomes `COMPLETED` via `setIsActive(false)` side-effect. `BookingStatus.CANCELLED` exists in enum but **no code path ever sets it**. |
| How is payment success recorded? | Only via `paymentMethod`, `transactionId`, `paymentTime` set in `processExitWithPayment`. There is **no explicit "payment successful" flag**; UPI payment is not verified — staff manually enters the last-5-digit txn ID. `processExit` (no payment) leaves all payment fields NULL. |
| Are payment timestamps reliable? | `paymentTime = LocalDateTime.now()` at exit processing — server time, consistent with `exitTime` (set separately in the same method → micro-differences). Reliable as "when staff recorded payment", not "when UPI settled". |
| How are fees calculated/stored? | `ceil(durationMinutes/60) * ₹20` → `parking_fee INTEGER`. `HOURLY_RATE` hardcoded in 2 services; `duration_minutes` column exists but is never populated. No per-vehicle-type or per-floor pricing. |
| How are slot statuses represented? | `AVAILABLE / LOCKED / OCCUPIED` enum on `parking_slots.status`; locks expire via scheduler. No historical record — status is overwritten in place. |
| Cancelled/expired bookings? | `CANCELLED` enum value exists, unused. Expired *locks* are handled (no booking exists). Expired *bookings* (vehicle never exits) are not handled — bookings stay `ACTIVE` forever. |
| Are booking records deleted? | Normally no. Exception: `POST /api/exit/cleanup-old-bookings` (`ExitService.cleanupOldBookings`) **hard-deletes** legacy bookings with short numeric codes — analytics must tolerate gaps. |
| Historical occupancy data? | **None.** Only current `parking_slots.status`. No snapshots, no event log, no time-series. Analytics for past dates can only be reconstructed approximately from `bookings.booking_time`/`exit_time` intervals; **occupancy *between* events and for the current moment is not recoverable historically**. |
| PII | `vehicleNumber`, `customerName`, `phoneNumber` are stored in plaintext; existing code masks them in sync DTOs. Analytics/report exports must decide masking policy. |

Additional observations:
- `duration_minutes` is never written — must be computed from `booking_time`/`exit_time` or backfilled.
- `processExit` does not validate/set `status` explicitly; relies on `setIsActive` side effects.
- `booking_code` is `VARCHAR(5)`, `updatable=false`, unique (nullable unique).
- Two parallel exit endpoints (`/api/exit/process` and `/api/sync/exit/process`) — event hooks must cover both or be moved into the service layer (preferred).
- `DataSyncService.fetchFreshExitData` calls `exitService` → circular-ish dependency handled with `@Lazy`; analytics service should not couple to it.
- Server timezone = JVM default; all timestamps are `LocalDateTime` (no TZ). Analytics bucketing must use a consistent zone (recommend documenting server TZ or storing UTC going forward).

---

## 3. Proposed Database Changes (new Flyway migration `V5__Analytics_Schema.sql`)

### 3.1 `booking_events` — lifecycle event log
```sql
CREATE TABLE booking_events (
    id            BIGSERIAL PRIMARY KEY,
    booking_id    BIGINT REFERENCES bookings(id),   -- null allowed for orphan-safe inserts
    booking_code  VARCHAR(5),
    event_type    VARCHAR(30) NOT NULL,             -- BOOKED, ENTRY_CONFIRMED, EXIT_PROCESSED,
                                                  -- PAYMENT_RECORDED, LOCK_EXPIRED, BOOKING_CANCELLED
    event_time    TIMESTAMP NOT NULL,
    slot_id       VARCHAR(255),
    floor         INTEGER,
    vehicle_number VARCHAR(255),
    vehicle_type  VARCHAR(20),
    amount        INTEGER,                          -- fee when relevant
    payment_method VARCHAR(20),
    metadata      TEXT                               -- JSON, optional
);
CREATE INDEX idx_booking_events_time   ON booking_events(event_time);
CREATE INDEX idx_booking_events_booking ON booking_events(booking_id);
CREATE INDEX idx_booking_events_type   ON booking_events(event_type, event_time);
```
- Written by a new `BookingEventService` invoked inside the same `@Transactional` boundaries as existing flows.
- Also backfilled once from existing `bookings` rows (BOOKED from `booking_time`, EXIT/PAYMENT from `exit_time`/`payment_time`) via a one-off `CommandLineRunner` or migration-time `INSERT ... SELECT` guarded by "only when booking_events is empty".

### 3.2 `occupancy_snapshots` — periodic occupancy time-series
```sql
CREATE TABLE occupancy_snapshots (
    id               BIGSERIAL PRIMARY KEY,
    snapshot_time    TIMESTAMP NOT NULL,
    floor            INTEGER,                       -- null = all floors
    total_slots      INTEGER NOT NULL,
    occupied_count   INTEGER NOT NULL,
    locked_count     INTEGER NOT NULL,
    available_count  INTEGER NOT NULL,
    active_bookings  INTEGER NOT NULL,
    UNIQUE (snapshot_time, floor)
);
CREATE INDEX idx_occupancy_time ON occupancy_snapshots(snapshot_time);
```
- Populated by a scheduled job (recommended: every 1–5 min; configurable via `parking.analytics.snapshot-interval`).
- Granularity trade-off: 1-min snapshots ≈ 1440 rows/day/floor → ~86k rows/month. Acceptable; can downsample for analytics queries.

### 3.3 Optional: `analytics_daily_rollups` (only if query perf demands it)
Daily aggregates per floor/vehicle-type/payment-method to speed dashboard queries. Defer until profiling shows need.

### 3.4 Booking entity additions (nullable, migration-backed)
- `duration_minutes` — start writing it in both exit paths (currently never set).
- `entry_time` — **do not add**; `booking_time` already serves as entry. Document the equivalence instead.
- Optional `cancelled_time`/`cancel_reason` — only if cancellation feature is in scope (currently unsupported).

---

## 4. Files to Modify

| File | Change |
|---|---|
| `service/ParkingService.java` | `lockSlot` → emit `SLOT_LOCKED` event (optional); `bookSlot` → emit `BOOKED` event after save (inside existing tx). Extract `HOURLY_RATE` to a shared `ParkingFeeService`/constant to remove duplication with `ExitService`/`DataSyncService` (optional but recommended). |
| `service/ExitService.java` | `processExit` → emit `EXIT_PROCESSED` (+ `PAYMENT_RECORDED` with `method=CASH/UNPAID` semantics or separate `EXIT_WITHOUT_PAYMENT`); `processExitWithPayment` → emit `EXIT_PROCESSED` + `PAYMENT_RECORDED`; set `durationMinutes`; consider emitting events for both paths. |
| `service/DataSyncService.java` | No event writes needed (it only reads). Optionally reuse new `AnalyticsService` for stats. |
| `filter/AuthInterceptor.java` | Extend protected prefix list to include `/api/analytics/**` and `/analytics.html`. |
| `config/WebConfig.java` | Register `/analytics.html` and `/api/analytics/**` in interceptor path patterns. |
| `enums/` | Add `BookingEventType` enum (or store as String). Optionally add `PAYMENT_SUCCESS` semantics. |
| `repository/BookingRepository.java` | Add analytics queries: date-range completed bookings, revenue grouped by day/hour, counts by vehicle type/status, peak-hour grouping. Prefer native/JPQL grouped projections over loading entities. |
| New repositories | `BookingEventRepository`, `OccupancySnapshotRepository`. |
| `application-dev/prod.properties` | `parking.analytics.snapshot-interval`, `parking.analytics.enabled`, CSV export limits. |
| Static pages | Add "Analytics" item to sidebar nav in `index.html`, `exit.html`, `ground-floor.html`, `floor1.html` (consistent markup). |

## 5. New Files

- `entity/BookingEvent.java`, `entity/OccupancySnapshot.java`
- `repository/BookingEventRepository.java`, `repository/OccupancySnapshotRepository.java`
- `service/BookingEventService.java` — `recordEvent(...)`, PII-safe variants.
- `service/OccupancySnapshotService.java` — scheduled capture + backfill-from-bookings reconstruction.
- `service/AnalyticsService.java` — all aggregation logic (revenue, utilization, vehicles, ops, peak hours, reports).
- `controller/AnalyticsController.java` — REST endpoints under `/api/analytics`.
- `scheduler/OccupancySnapshotScheduler.java` (or `@Scheduled` inside service).
- `config/AnalyticsBackfillRunner.java` (one-shot `CommandLineRunner`/`ApplicationRunner` for seeding `booking_events` from historical bookings, idempotent).
- `static/analytics.html` — dashboard page using `design-system.css` + sidebar.
- `dto/analytics/*` — request/response DTOs (date ranges, breakdowns, report rows) instead of raw `Map<String,Object>` where practical.
- Tests: `AnalyticsServiceTest`, `BookingEventServiceTest`, `AnalyticsControllerTest`, snapshot scheduler test.

## 6. Analytics API Structure (proposal)

Base: `/api/analytics` — all GET, all session-protected, all support `from`/`to` (ISO date or datetime) params with server-side max range (e.g. 366 days) and default = today/last-30-days.

```
GET /api/analytics/summary?from&to            → KPIs: revenue, bookings, avg duration, occupancy %, active now
GET /api/analytics/revenue?from&to&groupBy=day|hour|paymentMethod
GET /api/analytics/utilization?from&to&floor= → slot-hours occupied/available/locked, per-floor
GET /api/analytics/vehicles?from&to           → counts by vehicleType, repeat visitors (vehicle_number), avg stay per type
GET /api/analytics/operations?from&to         → throughput (entries/exits per hour), avg turn time, currently parked, overdue bookings
GET /api/analytics/peak-hours?from&to&top=10  → entries & revenue by hour-of-day and day-of-week heatmap data
GET /api/analytics/bookings/history?from&to&status=&vehicleType=&paymentMethod=&page=&size=  → paginated transaction report
GET /api/analytics/bookings/{id}              → single booking detail (masked PII by default)
GET /api/analytics/occupancy/timeline?from&to&floor=  → snapshot series for charts
GET /api/analytics/reports/transactions.csv?from&to  → CSV export (streaming, capped rows, audit-logged)
GET /api/analytics/reports/revenue.csv?from&to
```

Notes:
- CSV export: use `StreamingResponseBody`/direct `HttpServletResponse` write, `Content-Disposition: attachment`, UTF-8 BOM for Excel, escape commas/quotes, cap at e.g. 50k rows with a notice.
- All monetary values returned as `BigDecimal`/paise-safe types; `parking_fee` is INTEGER ₹ — document rounding (`Math.round`).
- Masking: reuse the masking approach from `DataSyncService.convertBookingToMap` (`***XXXX` patterns); full PII only if a future role check allows (currently single role — keep masked by default, add `includePii=true` only behind explicit staff decision).

## 7. Occupancy Snapshot Design

- **Capture**: `@Scheduled(fixedDelayString = "${parking.analytics.snapshot-interval:PT1M}")` — count slots grouped by `(floor, status)` in one grouped query + `countByIsActiveTrue` for active bookings. Store one row per floor plus a `floor=NULL` total row (or compute totals in queries — prefer storing totals to keep timeline queries cheap).
- **Backfill**: for historical dates, reconstruct occupancy by replaying booking intervals (`booking_time`→`exit_time`) over snapshot grid — approximate; mark reconstructed rows `metadata.source='reconstructed'` if a metadata column is added, or keep a separate flag. Current-day "now" occupancy comes from live `parking_slots`.
- **Usage**: utilization %, peak occupancy times, floor-level heatmaps, "currently occupied" KPI without scanning bookings.

## 8. Booking Lifecycle Event Insertion Points

| Event | Insertion point | Transaction |
|---|---|---|
| `SLOT_LOCKED` (optional) | `ParkingService.lockSlot` after `parkingSlotRepository.save(slot)` | existing `@Transactional` |
| `BOOKED` | `ParkingService.bookSlot` after `bookingRepository.save` + slot update | existing tx — must be same tx so event and booking commit atomically |
| `LOCK_EXPIRED` | `ParkingService.releaseExpiredLocks` / `LockReleaseScheduler` (slot-only; no booking_id) | existing tx |
| `EXIT_PROCESSED` | `ExitService.processExit` and `processExitWithPayment` after `bookingRepository.save` | existing tx |
| `PAYMENT_RECORDED` | `ExitService.processExitWithPayment` after save (method, txnId, amount) | same tx |
| `BOOKING_CANCELLED` | future cancellation endpoint — reserve enum value now | — |

Implementation detail: inject `BookingEventService` into the services; wrap event writes in try/catch that logs but does **not** roll back the business transaction? — **Decision needed**: recommend letting event insert failures propagate within the same tx for consistency (events are part of the audit trail), or use `REQUIRES_NEW`+retry if availability of analytics must never block parking ops. Plan default: same transaction (consistency-first), with a config flag `parking.analytics.events-required=false` to downgrade to best-effort logging if ops prefer availability.

## 9. Security Implementation Plan

1. Extend `AuthInterceptor` protected paths: `/api/analytics/**`, `/analytics.html` (and optionally the other static pages — out of scope).
2. Register paths in `WebConfig.addInterceptors`.
3. Role-based access: `StaffUser.role` currently unused. Introduce `@PreAuthorize`-style check inside `AnalyticsController` via session attribute (`AuthService.getCurrentUser`) — require authenticated staff; optionally restrict CSV/full-PII exports to `ADMIN` role. Document as phase 2 if role model is not yet trusted.
4. Rate limiting: existing `RateLimitingFilter`/`TokenBucket` — verify analytics endpoints inherit global limits; CSV exports should get stricter limits.
5. Input validation: strict `from`/`to` parsing, max range enforcement, parameterized queries only (Spring Data), no string-concatenated SQL.
6. PII: mask by default in all analytics payloads and CSVs; log access to export endpoints.
7. CORS: keep `@CrossOrigin` consistent with existing controllers; analytics inherits session auth so `allowCredentials=false` + cookie session is a known mismatch — verify session cookie `SameSite` behavior in browsers (existing exit page already relies on it).

## 10. Testing Plan

- **Unit**: `AnalyticsService` aggregation math (revenue sums, ceil-hour fee parity with `HOURLY_RATE`, peak-hour bucketing, empty-range behavior); `BookingEventService` event payloads; snapshot counting vs seeded slots.
- **Integration (MockMvc, extend existing style)**: `AnalyticsControllerTest` — 401 without session for `/api/analytics/**`; 200 with session; CSV content-type + escaping; date-range validation errors.
- **Repository tests** (`@DataJpaTest` + H2 or Testcontainers Postgres — project currently has no DB tests; recommend Testcontainers since queries are Postgres-specific): grouped revenue/utilization queries.
- **Regression**: run existing `ParkingControllerTest`, `AuthControllerTest`; verify `bookSlot`/`processExit` flows still pass with event writes (event insert failure path).
- **Migration test**: Flyway migrate on clean DB + on a copy of dev DB; verify `booking_events` backfill counts match bookings.
- **Manual**: book → exit → check events rows; run snapshot job; open `analytics.html`; export CSV.

## 11. Risks & Compatibility Considerations

- **`bookingTime` == entry time**: document everywhere; do not introduce a misleading separate "entry" concept unless product requires walk-in vs booked distinction.
- **No payment verification**: "revenue" = fees recorded at exit, not confirmed UPI settlements. Label dashboards accordingly ("Recorded Revenue").
- **`duration_minutes` unused**: populate going forward; backfill `UPDATE bookings SET duration_minutes = EXTRACT(EPOCH FROM (exit_time - booking_time))/60 WHERE exit_time IS NOT NULL` in V5 (idempotent).
- **Hard deletes**: `cleanupOldBookings` deletes rows — analytics history will permanently lose those bookings; consider disabling cleanup or archiving before delete (flag as risk/follow-up).
- **`is_active`/`status` duality**: `setIsActive`/`setStatus` keep them in sync, but direct `isActive` queries are used in repositories — analytics should treat "completed" = `exitTime IS NOT NULL` (most reliable) and note `status` may drift for legacy rows.
- **Timezone**: all `LocalDateTime`, JVM default zone. Peak-hour analytics inherit server TZ; document assumption; consider storing zone in snapshot metadata.
- **Duplicate exit paths**: `/api/exit/process`, `/api/sync/exit/process`, `/api/exit/process-payment` — event hooks placed in `ExitService` cover all three automatically (preferred over controller hooks).
- **Volume**: bookings table is small today; grouped JPQL is fine initially. Add rollup table only if needed.
- **Hibernate `ddl-auto=update` (dev)**: could create tables before Flyway — keep `ddl-auto=validate` recommendation or ensure Flyway runs first; prod already `validate`.
- **Frontend**: pages are large static HTML; new `analytics.html` must reuse `design-system.css` classes and sidebar markup; add nav item consistently across 4 pages.
- **Caching**: do NOT route analytics through `DataSyncService`'s 8h cache — analytics needs fresh DB reads; use short-TTL Caffeine cache (e.g. 60s) keyed by query params if needed.
- **Booking code collisions/legacy nulls**: handle `booking_code NULL` rows in reports (fallback `ID-<id>` display, same as `ExitService`).

## 12. Suggested Implementation Order (for the next task)

1. `V5` migration: `booking_events`, `occupancy_snapshots`, `duration_minutes` backfill, indexes.
2. Entities + repositories + `BookingEventService`.
3. Hook events into `ParkingService.bookSlot`/`lockSlot` and `ExitService` exit paths (incl. `durationMinutes` write).
4. `AnalyticsBackfillRunner` for historical events.
5. `OccupancySnapshotService` + scheduler.
6. `AnalyticsService` + `AnalyticsController` + security wiring.
7. `analytics.html` dashboard + sidebar links + CSV export.
8. Tests + docs update.
