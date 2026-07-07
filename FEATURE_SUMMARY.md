# Smart Parking Management System - Feature Summary

This document details the functional and technical modules of the Smart Parking Management System.

---

## 🔐 1. Authentication
* **Session-based Authentication**: High-security staff login model using Spring `HttpSession` (no JWT overhead or token storage risks). Includes protection against session fixation attacks.
* **Credentials Hashing**: Salted password storage using the industry-standard BCrypt hashing function.
* **Access Control**: Handled via `AuthInterceptor` which intercepts traffic to `/api/exit/**` and `/exit.html`. Returns `401 Unauthorized` responses for unauthenticated requests.
* **Cache Control**: Employs cache-busting headers on the Exit Management Portal to prevent browser back-button navigation to protected dashboards post-logout.

---

## 🗺️ 2. Navigation & Interface
* **Responsive Layouts**: Fully responsive layouts built with modern CSS variables, supporting mobile, tablet, and desktop viewports.
* **Separate Floor Booking**: Distinct views for Ground Floor (`ground-floor.html`) and First Floor (`floor1.html`) containing interactive grid mapping.
* **Dashboard View**: A global landing dashboard (`index.html`) displaying global occupancy statistics and overview status.
* **User Feedback Controls**: Professional modals for booking forms, backdrop-blur transitions, real-time input validation, and feedback notifications.

---

## 🚗 3. Parking Slot Management
* **Total Capacity**: **600 Slots** divided equally across two levels:
  * **Ground Floor**: 300 slots (mapped to slot IDs `AG01-AG20` through `OG01-OG20`).
  * **First Floor**: 300 slots (mapped to slot IDs `AF01-AF20` through `OF01-OF20`).
* **Slot Layout & Identifiers**: Scaled structure utilizing floor-based and alphabetical grouping (15 areas A-O, 20 slots each).
* **Direct Access**: Unauthenticated guest access for booking activities.
* **Temporary Slot Locking**: Pre-booking lock mechanism for **2 minutes** to prevent double-booking. Schedulers clear expired locks automatically.

---

## 📅 4. Parking Booking
* **Normalized Booking Process**: Robust booking handling utilizing strict validation rules.
  * **Vehicle Number**: Must match format `XX00XX0000` (e.g., `KA01AB1234`).
  * **Phone Number**: Exact 10-digit formats.
  * **Customer Name**: Alphabetic names under 20 characters.
* **Duplicate Active Booking Block**: Partial database unique constraints prevent booking a vehicle that is currently active/parked.
* **Re-booking Support**: Same vehicle number can successfully book a slot again immediately after processing an exit.
* **Confirmation PDF/Text Generation**: Automatic booking slip downloads showing entry codes, slot details, and custom parking timestamps.

---

## 🚪 5. Exit Management Portal
* **Staff-Only Dashboard**: Protected entrance (`/exit.html`) containing the list of all currently active bookings.
* **Real-time Fee Engine**: Flat-rate calculation of **₹20.00/hour**, rounding up fractional hours to the next whole hour.
* **Dual Payment Processing**:
  * **Cash**: Handled by staff with manual completion.
  * **UPI Payments**: Dynamic, validation-checked UPI transaction flow.
* **UPI QR Code Generation**: Built-in QR engine (via ZXing library) generating base64 PNG images containing transaction details and merchant IDs. Requires validation of the last 5 digits of the transaction ID.
* **Exit Processing Workflow**: Releasing a vehicle updates the slot status to `AVAILABLE` and marks the booking as inactive.

---

## 📄 6. Receipt System
* **Dual Retrieval Options**: Downloadable receipts via backend endpoints by searching for either the booking ID or the alphanumeric booking code.
* **UTF-8 Encoding**: Explicit character encoding prevents text display issues.
* **Post-Payment Downloads**: Prompts receipt download options immediately after exit payments.
* **Exit History Downloads**: Download receipt options available for all released vehicles listed in the exit log table.
* **Frontend Fallback**: Automated JS fallbacks generate local receipts if connection issues prevent querying the database.

---

## ⏳ 7. Rate Limiting
* **Token Bucket Algorithm**: Standard token-bucket filter tracking IP addresses to protect key endpoints.
* **Optimized Limits**:
  * **Locking**: 50 lock attempts/hour.
  * **Bookings**: 30 booking attempts/hour.
  * **Page Views**: 200 views/hour.
  * **Receipts**: 50 downloads/hour.
  * **General**: 300 requests/hour.
* **Automatic Eviction**: Scheduler evicts unused token buckets from memory every 5 minutes to prevent leakages.

---

## 🔄 8. Real-time Synchronization
* **Cross-Tab Communication**: BroadcastChannel API sends instant update events across open tabs on slot releases, locks, and bookings.
* **Deduplication Engine**: Frontend filters out duplicated signals or repeated notifications.
* **Preservation of PII & Booking Data**: Ensures correct mapping of IDs, customer phone numbers, and payment transaction metadata across UI components.

---

## 🐳 9. Docker Support
* **Multi-Stage Build**: Employs `maven:3.9.6-eclipse-temurin-21` for building and lightweight `eclipse-temurin:21-jre` for runtime.
* **Runtime Customization**: Drops root privileges to run as user `parking`, configures Asia/Kolkata timezone, sets language locales, and runs JVM container-optimization properties.
* **Health Checks**: Employs standard runtime health checks.

---

## 🗄️ 10. Database
* **Relational Schema**: Structured PostgreSQL design containing optimized index paths.
* **Concurrency Protection**: Database row pessimistic locks prevent concurrent operations from double-locking parking slots.
* **Data Integrity Constraints**: Unique constraints on slot numbers and partial unique indexes for active vehicles.

---

## 🚀 11. Flyway Migrations
* **Schema Evolution**: Sequence-controlled migrations (V1 to V4) ensuring consistent database schema initialization and schema upgrades.
* **Automatic Verification**: Flyway validation matches checksums on application startup to ensure schema compatibility.

---

## ⚡ 12. Performance
* **Eager Joins & EntityGraph**: Optimized JPQL queries use `LEFT JOIN FETCH` or `@EntityGraph` annotations to eagerly retrieve `ParkingSlot` properties with `Booking` records, resolving N+1 queries.
* **No OSIV**: `spring.jpa.open-in-view=false` disables holding connection resources during view serialization.
* **Startup Performance**: Indexed count queries replacing legacy table scans drop startup and cache synchronization from **130 seconds down to ~5.3 seconds**.
* **Cache Management**: Caffeine cache holds static configurations.

---

## 🔒 13. Security
* **Session Protection**: Invalidating sessions upon logout/login protects against session hijacking and session fixation.
* **DDoS Prevention**: Token bucket limits secure REST endpoints.
* **Encryption**: Salted BCrypt password encoding.
* **PII Masking**: Masking customer names and phone numbers in cached exit synchronization records.

---

## 📊 14. Monitoring
* **Spring Boot Actuator**: Native endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/info`) expose system status and performance metrics.
* **Health Indicators**: Dynamic database connectivity, disk space, and application checks.

---

## ☁️ 15. Cloud Deployment
* **Neon PostgreSQL Integration**: Utilizes Neon Serverless PostgreSQL with SSL connection pools.
* **Render Deployment**: Dockerized web service deployment bound to port `10000` with automated Render builds.
