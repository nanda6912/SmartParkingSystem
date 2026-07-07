# Smart Parking Management System - End-to-End Deployment Guide

This guide details the step-by-step processes to deploy the Smart Parking Management System, covering local Docker configurations and cloud deployment using Render and Neon Serverless PostgreSQL.

---

## 💻 1. Local Container Deployment

### Step 1: Clone the Repository
Clone the repository and navigate into the project root directory:
```bash
git clone <repository-url>
cd SmartParkingSystem
```

### Step 2: Configure Environment Variables
Copy the template configuration file to create `.env`:
```bash
cp .env.example .env
```
Open `.env` in a text editor (e.g., `nano .env`) and update variables:
* Set `DB_PASSWORD` and `SPRING_DATASOURCE_PASSWORD` to a secure custom password.
* Set the port parameters (`SERVER_PORT`, `ACTUATOR_PORT`, `DB_PORT`) if you have port conflicts.
* Configure `UPI_MERCHANT_ID` and `UPI_MERCHANT_NAME` with merchant identifiers.

### Step 3: Build the Application & Docker Image
Trigger the container build. This compiles code, downloads dependencies, and builds the container runtime:
```bash
docker compose up --build -d
```

### Step 4: Start Containers
Start the multi-container configuration:
```bash
docker compose up -d
```

### Step 5: Verify Application Status
Verify that containers are up and running:
```bash
docker compose ps
```
Perform a curl request to verify backend health:
```bash
curl http://localhost:8081/actuator/health
```
You should receive a `{"status":"UP"}` JSON response.

### Step 6: Verify Flyway Migration Status
Connect to the database container CLI:
```bash
docker compose exec parking-db psql -U postgres -d smart_parking_db
```
Verify migrations were successfully applied by inspecting the schema history table:
```sql
SELECT version, description, success FROM flyway_schema_history;
```
It should display versions `1`, `2`, `3`, and `4` as successfully applied.

---

## 🐘 2. Cloud Database Setup (Neon PostgreSQL)

Production deployments use Neon PostgreSQL for scalable and serverless hosting:

1. **Create Neon Project**: Log into [Neon Console](https://neon.tech/) and create a new project.
2. **Retrieve Connection String**: Copy the provided JDBC connection string. It will look like:
   `jdbc:postgresql://ep-example-123456.us-east-2.aws.neon.tech/neondb?sslmode=require`
3. **Database Credentials**: Note the server user name, host domain name, database name, and password.
4. **Initial Schema**: Flyway executes migration scripts automatically on application startup. You do not need to create tables manually.

---

## ☁️ 3. Production Deployment (Render Web Service)

Deploy the system as a Docker Web Service on Render:

1. **Connect Repository**: Go to [Render Dashboard](https://dashboard.render.com/), click **New** -> **Web Service**, and link your GitHub repository.
2. **Environment/Language**: Select **Docker** as the deployment runtime. Render will automatically read the root-level `Dockerfile` and execute its multi-stage instructions.
3. **Region**: Select the region closest to your users (and matching your Neon database region to reduce network latency).
4. **Plan**: Select your target plan (e.g. Free or Starter).

---

## ⚙️ 4. Required Production Environment Variables (Render)

Configure these key-value pairs in the **Environment** section of your Render Web Service settings:

| Key | Value | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production database properties. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host-url>/neondb?sslmode=require` | Your Neon database URL. |
| `SPRING_DATASOURCE_USERNAME` | `<neon-db-user>` | Neon database user name. |
| `SPRING_DATASOURCE_PASSWORD` | `<neon-db-password>` | Neon database connection password. |
| `UPI_MERCHANT_ID` | `nandakumar27@ptyes` | Production UPI ID for payment QR codes. |
| `UPI_MERCHANT_NAME` | `Smart Parking System` | Payment display name. |

> [!IMPORTANT]
> The container JRE configuration defaults the application port to `10000`. Render automatically routes external traffic to container port `10000`, so no port mapping is required.

---

## 🔄 5. Updates & Redeployment Workflow

### Step 1: Commit Local Code Modifications
Commit your changes and push them to your target deployment branch (e.g. `main`):
```bash
git add .
git commit -m "Optimize query fetches and disable OSIV"
git push origin main
```

### Step 2: Render Automated Deployments
By default, Render has **Auto-Deploy** enabled. Pushing commits to your repository triggers a build and redeployment.
If Auto-Deploy is disabled, go to the Render dashboard and click **Manual Deploy** -> **Clear Build Cache & Deploy**.

### Step 3: Monitor Deploy Progress
1. Watch the Render compilation logs to ensure JRE packaging completes.
2. Check startup logs for connection metrics:
   `Started SmartParkingApplication in X seconds`
3. Verify that Flyway executes schema checks without validation errors.

---

## ❌ 6. Cloud Troubleshooting

* **Connection Pool Blocked**:
  * *Symptom*: Application logs show database connection time outs.
  * *Fix*: Verify that `sslmode=require` is present in the `SPRING_DATASOURCE_URL` configuration. Ensure your Neon instance is active and not paused due to inactivity.
* **Out Of Memory (OOM) Container Crash**:
  * *Symptom*: Web service restarts repeatedly with code `137` (SIGKILL).
  * *Fix*: Render free tier limits memory to 512MB. If you exceed this, adjust the `JAVA_OPTS` setting in your environment variables to use lower heap boundaries (e.g. `-Xmx256m -Xms128m`).
* **Flyway Migration Checksum Mismatches**:
  * *Symptom*: Startup logs show `Migration checksum mismatch` for applied versions.
  * *Fix*: Never edit files inside `src/main/resources/db/migration/` after they are applied to production. If an index or constraint changes, write a new migration file (e.g. `V5__New_Index.sql`).
