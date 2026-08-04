# ReconX — Enterprise Trade Reconciliation Platform

ReconX is a full-stack trade reconciliation system built during the Advanced Track of DB26.
It ingests trade events via Kafka, persists them to PostgreSQL, detects reconciliation breaks,
and surfaces real-time alerts and dashboards through a React SPA backed by a Spring Boot API.

---

## For Students — Start Here

> Read this section first before doing anything else.
> Every command is provided for both **macOS (Terminal)** and **Windows (PowerShell)**.

This is Day 10 — the final day. You are going to:
1. Apply all the solved files from this folder into your project
2. Configure the GitHub Actions CI pipeline
3. Boot the full stack with Docker
4. Run the load test and watch the Grafana dashboard
5. Tag `v1.0.0` and ship

---

### What You Need Installed (one-time setup)

Install these tools **before** you start. Check each one after installing.

#### Docker Desktop
Required to run the full 7-service stack locally.

- Download: https://www.docker.com/products/docker-desktop/
- After install, open Docker Desktop and wait for the whale icon to show "Engine running"
- Allocate at least **4 GB RAM** to Docker: Docker Desktop → Settings → Resources → Memory → 4 GB → Apply

**Verify:**

| macOS | Windows (PowerShell) |
|-------|----------------------|
| `docker --version` | `docker --version` |
| `docker compose version` | `docker compose version` |

Expected output: `Docker version 25+` and `Docker Compose version v2+`

---

#### Java 21 (Temurin JDK)
Required to build the backend JAR with Maven.

- Download: https://adoptium.net/temurin/releases/?version=21
- On the page: select **Version 21**, your OS, your Architecture (x64 for most), **JDK**, `.pkg` (macOS) or `.msi` (Windows) → Download and install

**Verify:**

| macOS | Windows (PowerShell) |
|-------|----------------------|
| `java -version` | `java -version` |

Expected output: `openjdk version "21.x.x"`

> **Windows tip:** After installing Java, close and reopen PowerShell so the `PATH` update takes effect.

---

#### Git
Required to clone the repo and push commits/tags.

**macOS:** Git is usually pre-installed. If not:
```bash
brew install git
```

**Windows:** Download and install Git for Windows from https://git-scm.com/download/win
- During install: choose **"Git from the command line and also from 3rd-party software"**
- This also installs Git Bash (useful if PowerShell gives you trouble)

**Verify:**

| macOS | Windows (PowerShell) |
|-------|----------------------|
| `git --version` | `git --version` |

---

#### k6 (Load Testing Tool)
Required for the load test in Step 11.

**macOS:**
```bash
brew install k6
```

**Windows (PowerShell — run as Administrator):**
```powershell
winget install k6
# or if you have Chocolatey:
choco install k6
```

**Verify:**

| macOS | Windows (PowerShell) |
|-------|----------------------|
| `k6 version` | `k6 version` |

Expected output: `k6 v0.50+`

---

### Step 1 — Clone the project (if you haven't already)

**macOS:**
```bash
git clone https://github.com/<your-org>/reconx.git
cd reconx
```

**Windows (PowerShell):**
```powershell
git clone https://github.com/<your-org>/reconx.git
cd reconx
```

> Replace `<your-org>` with your GitHub username or organisation name.

---

### Step 2 — Apply the Day 10 solved files into your project

Run these commands from the **project root** (the folder that contains `docker-compose.yml`).

**macOS:**
```bash
# Infra files
cp day10-solved-files/docker-compose.yml .
cp day10-solved-files/backend/Dockerfile backend/
cp day10-solved-files/frontend/Dockerfile frontend/
cp day10-solved-files/frontend/nginx.conf frontend/
cp -R day10-solved-files/monitoring .

# Backend build + quality
cp day10-solved-files/backend/pom.xml backend/
cp day10-solved-files/backend/checkstyle.xml backend/

# CI pipeline
mkdir -p .github/workflows
cp day10-solved-files/.github/workflows/ci.yml .github/workflows/

# Load test + smoke test
mkdir -p load-tests scripts
cp day10-solved-files/load-tests/trade-creation.js load-tests/
cp day10-solved-files/scripts/smoke-test.sh scripts/
chmod +x scripts/smoke-test.sh

# Docs + README
cp day10-solved-files/README.md .
mkdir -p docs db/diagrams
cp day10-solved-files/docs/demo-deck.md docs/
cp day10-solved-files/docs/demo-runsheet.md docs/
cp day10-solved-files/docs/retro.md docs/
cp day10-solved-files/db/diagrams/architecture.md db/diagrams/
```

**Windows (PowerShell):**
```powershell
# Infra files
Copy-Item day10-solved-files\docker-compose.yml .
Copy-Item day10-solved-files\backend\Dockerfile backend\
Copy-Item day10-solved-files\frontend\Dockerfile frontend\
Copy-Item day10-solved-files\frontend\nginx.conf frontend\
Copy-Item -Recurse day10-solved-files\monitoring . -Force

# Backend build + quality
Copy-Item day10-solved-files\backend\pom.xml backend\
Copy-Item day10-solved-files\backend\checkstyle.xml backend\

# CI pipeline
New-Item -ItemType Directory -Force -Path .github\workflows
Copy-Item day10-solved-files\.github\workflows\ci.yml .github\workflows\

# Load test + smoke test
New-Item -ItemType Directory -Force -Path load-tests, scripts
Copy-Item day10-solved-files\load-tests\trade-creation.js load-tests\
Copy-Item day10-solved-files\scripts\smoke-test.sh scripts\

# Docs + README
Copy-Item day10-solved-files\README.md .
New-Item -ItemType Directory -Force -Path docs, db\diagrams
Copy-Item day10-solved-files\docs\demo-deck.md docs\
Copy-Item day10-solved-files\docs\demo-runsheet.md docs\
Copy-Item day10-solved-files\docs\retro.md docs\
Copy-Item day10-solved-files\db\diagrams\architecture.md db\diagrams\
```

---

### Step 3 — Configure the GitHub Actions CI pipeline

**3a. Enable write permissions for GitHub Actions on your repo:**

1. Open your GitHub repository in the browser
2. Go to **Settings** → **Actions** → **General**
3. Under **Workflow permissions** → select **Read and write permissions**
4. Click **Save**

> Without this step the Docker push job will fail with `403 Forbidden`.

**3b. Push the CI file to GitHub to activate the pipeline:**

**macOS:**
```bash
git add .github/workflows/ci.yml
git commit -m "Add CI pipeline"
git push
```

**Windows (PowerShell):**
```powershell
git add .github/workflows/ci.yml
git commit -m "Add CI pipeline"
git push
```

Go to your repo on GitHub → **Actions** tab → you should see a workflow run starting.

---

### Step 4 — Build the backend JAR

The Docker build copies the compiled JAR from `target/` — build it first.

**macOS:**
```bash
cd backend
./mvnw -q clean package -DskipTests
cd ..
```

**Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd -q clean package "-DskipTests"
cd ..
```

Expected: `BUILD SUCCESS` printed at the end.

---

### Step 5 — Start the full stack

**macOS:**
```bash
docker compose up -d --build
```

**Windows (PowerShell):**
```powershell
docker compose up -d --build
```

> First run takes 3–5 minutes (Docker downloads all base images and builds your app).
> Subsequent runs take ~30 seconds.

---

### Step 6 — Check that all services are healthy

**macOS:**
```bash
watch -n 2 'docker compose ps'
# Press Ctrl+C once everything is healthy
```

**Windows (PowerShell):**
```powershell
while ($true) { docker compose ps; Start-Sleep 3; Clear-Host }
# Press Ctrl+C once everything is healthy
```

All 7 services should reach this state:

| Container | Status |
|-----------|--------|
| reconx-postgres | `healthy` |
| reconx-zookeeper | `running` |
| reconx-kafka | `healthy` |
| reconx-backend | `healthy` |
| reconx-frontend | `running` |
| reconx-prometheus | `running` |
| reconx-grafana | `running` |

---

### Step 7 — Open the app in your browser

| What | URL | Login |
|------|-----|-------|
| Frontend SPA | http://localhost:5173 | admin / admin |
| Backend API docs | http://localhost:8081/api/swagger-ui.html | — |
| Grafana dashboards | http://localhost:3000 | admin / admin |
| Prometheus metrics | http://localhost:9090 | — |

---

### Step 8 — Run the quality gates locally

**macOS:**
```bash
cd backend
./mvnw verify -Pci
open target/site/jacoco/index.html
cd ..
```

**Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd verify "-Pci"
start target\site\jacoco\index.html
cd ..
```

The build must pass with:
- JaCoCo line coverage **≥ 85%** (check the HTML report)
- Checkstyle **0 violations** (any violation fails the build with a clear message)

---

### Step 9 — Run the k6 load test

Make sure the stack from Step 5 is still running.

**macOS:**
```bash
k6 run load-tests/trade-creation.js
```

**Windows (PowerShell):**
```powershell
k6 run load-tests\trade-creation.js
```

Watch **Grafana → ReconX Overview** while k6 runs. After 60 seconds you should see:
- `http_req_failed` **< 1%**
- `p(95)` latency **< 800ms**

---

### Step 10 — Tag the release

Run this after CI is green on `main`.

**macOS:**
```bash
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
git push origin v1.0.0
```

**Windows (PowerShell):**
```powershell
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
git push origin v1.0.0
```

CI will automatically build and push:
- `ghcr.io/<your-org>/reconx-backend:1.0.0`
- `ghcr.io/<your-org>/reconx-frontend:1.0.0`

---

### Step 11 — Tear down when done

**macOS:**
```bash
docker compose down -v
```

**Windows (PowerShell):**
```powershell
docker compose down -v
```

> `-v` removes all data volumes (Postgres data, Grafana state). Omit it if you want to keep the data.

---

### Common Problems and Fixes

| Problem | Fix |
|---------|-----|
| `docker compose up` hangs or a container exits immediately | Run `docker compose logs <service-name>` to read the error |
| Backend exits with `JWT_SECRET` error | Open `docker-compose.yml`, find `backend.environment`, add `JWT_SECRET: any-string-32-chars-min` |
| Grafana dashboards show "No data" | The Prometheus datasource URL must be `http://prometheus:9090` not `localhost:9090` — check `monitoring/grafana/provisioning/datasources/prometheus.yml` |
| Prometheus shows backend target as DOWN | The scrape target must be `backend:8081` not `localhost:8081` — check `monitoring/prometheus/prometheus.yml` |
| `./mvnw: Permission denied` (macOS) | Run `chmod +x backend/mvnw` then retry |
| `mvnw.cmd` not recognised (Windows) | Make sure you are inside the `backend` folder and Java 21 is on your PATH |
| JaCoCo coverage below 85% | Add unit tests for exception handling paths, then re-run `mvn verify -Pci` |
| Checkstyle build failure | Read the error — it shows file name + line number + rule. Fix the formatting issue and re-run |
| k6 reports lots of 401 errors | The JWT token expired during the run — the provided script already handles this via `setup()`, so make sure you are running the file from `load-tests/trade-creation.js` |
| `git push` rejected on tag | The tag already exists remotely — delete it first: `git push origin --delete v1.0.0` then re-tag |
| CI `docker` job fails with 403 | Go to repo Settings → Actions → General → set Workflow permissions to **Read and write** |

---

## Architecture

See [`db/diagrams/architecture.md`](db/diagrams/architecture.md) for full C4 diagrams
(Context → Container → Component levels).

```
Browser (React SPA)
      │  HTTPS :5173
      ▼
  nginx proxy  ──/api/──►  Spring Boot API :8081
                                  │
                   ┌──────────────┼──────────────┐
                   ▼              ▼              ▼
             PostgreSQL 16    Apache Kafka    Prometheus
              (trades, recon,  (trade-events,   (metrics
               audit, envers)   recon-results,   scrape)
                                audit-events,       │
                                dlq)            Grafana
```

---

## Quick Start (Docker Compose)

**Prerequisites:** Docker Desktop 4.x+, at least 4 GB RAM allocated to Docker.

```bash
# 1. Build the backend JAR (Dockerfile picks up target/*.jar)
cd backend && ./mvnw -q clean package -DskipTests && cd ..

# 2. Start the full 7-service stack
docker compose up -d --build

# 3. Watch services become healthy (~30-60s)
watch 'docker compose ps'
```

Once all services show `(healthy)`:

| Service | URL |
|---------|-----|
| Frontend SPA | <http://localhost:5173> |
| Backend Swagger UI | <http://localhost:8081/api/swagger-ui.html> |
| Prometheus | <http://localhost:9090> |
| Grafana | <http://localhost:3000> (admin / admin) |
| Kafdrop (debug profile) | `docker compose --profile debug up -d` → <http://localhost:9000> |

**Tear down (removes volumes):**
```bash
docker compose down -v
```

---

## Running Each Layer

### Backend only (dev mode)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Swagger: http://localhost:8081/api/swagger-ui.html
```

### Frontend only (dev server)
```bash
cd frontend
npm install
npm run dev
# SPA: http://localhost:5173
```

### Tests
```bash
cd backend
./mvnw test                    # unit tests only
./mvnw verify -Pci             # full: tests + JaCoCo 85% gate + Checkstyle
open target/site/jacoco/index.html
```

### k6 Load Test
```bash
# Requires: brew install k6
# Stack must be running first.
k6 run load-tests/trade-creation.js
```

---

## Project Structure

```
reconx/
├── backend/                  Spring Boot 3 / Java 21
│   ├── src/main/java/…       Controllers, Services, Kafka, Security, Observability
│   ├── src/main/resources/   application*.yml, Liquibase changelogs
│   ├── src/test/             Unit + Integration tests (Testcontainers)
│   ├── Dockerfile            Multi-stage: eclipse-temurin:21-jdk → jre-alpine
│   ├── checkstyle.xml        Google-style Checkstyle ruleset
│   └── pom.xml               Dependencies + JaCoCo + Checkstyle plugins
├── frontend/                 React + Vite
│   ├── src/                  Components, hooks, API client
│   ├── Dockerfile            Multi-stage: node:22-alpine → nginx:1.27-alpine
│   └── nginx.conf            SPA fallback + /api/ proxy to backend
├── monitoring/
│   ├── prometheus/           prometheus.yml + alerts.yml
│   └── grafana/provisioning/ Datasource + dashboard JSON
├── load-tests/
│   └── trade-creation.js     k6 script — 200 VUs × 60s
├── scripts/
│   └── smoke-test.sh         Bring-up → health wait → endpoint checks → teardown
├── db/diagrams/              C4 Mermaid architecture diagrams
├── docs/
│   ├── demo-runsheet.md      20-minute demo script
│   └── retro.md              Sprint retrospective
├── docker-compose.yml        Full 7-service stack
└── .github/workflows/ci.yml  GitHub Actions CI pipeline
```

---

## CI / CD

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push to `main` and on version tags:

1. **Liquibase validate** — confirms changelogs are consistent with the schema before any tests run.
2. **Maven verify** (`-Pci`) — runs all tests, enforces the JaCoCo 85% line-coverage gate, and runs Checkstyle.
3. **Docker build & push** — builds backend and frontend images, tags them with the semver or SHA, and pushes to GHCR.

### Release

```bash
# After CI is green on main:
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
git push origin v1.0.0
# CI builds ghcr.io/<org>/reconx-backend:1.0.0 and reconx-frontend:1.0.0
```

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Kafka for trade events | Decouples producers from reconciliation consumers; enables replay |
| Dead-letter queue (DLQ) | Poison-pill messages don't block the main pipeline |
| Hibernate Envers | Full audit trail for every trade mutation — no custom trigger SQL |
| Testcontainers for ITs | Tests run against real Postgres + Kafka; no mock divergence |
| JaCoCo 85% gate | Enforces meaningful test coverage in CI; excludes generated mappers/DTOs |
| Checkstyle (Google style) | Keeps code readable across the team; max 120 chars/line |
| Caffeine cache | In-memory cache for instruments and counterparties; evicts after 5 min |
| Micrometer + Prometheus | Zero-overhead metrics via Spring Boot Actuator |

---

## Final Day Demo — Step by Step

Follow these steps in order on demo day. Allow **5–10 minutes** for the stack to build on first run.

> Commands are shown for **Mac (Terminal / zsh)** and **Windows (PowerShell)** side by side.
> Windows users: make sure **Docker Desktop**, **Git for Windows**, and **Java 21** are installed.
> On Windows, run PowerShell as a regular user (no Administrator needed for Docker Desktop).

---

### Prerequisites — install once before demo day

| Tool | Mac | Windows |
|------|-----|---------|
| Docker Desktop | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | Same link — install and start Docker Desktop |
| Java 21 (Temurin) | `brew install --cask temurin@21` | Download from [adoptium.net](https://adoptium.net) and run the installer |
| k6 load tester | `brew install k6` | `winget install k6` or `choco install k6` |
| Git | `brew install git` (usually pre-installed) | [git-scm.com](https://git-scm.com/download/win) — includes Git Bash |

Verify everything is ready:

**Mac — Terminal:**
```bash
docker --version        # Docker version 25+
java -version           # openjdk 21
k6 version              # k6 v0.50+
git --version
```

**Windows — PowerShell:**
```powershell
docker --version
java -version
k6 version
git --version
```

---

### Step 1 — Open a terminal in the project root

**Mac:**
```bash
cd ~/path/to/reconx
# or drag the project folder onto Terminal
```

**Windows (PowerShell):**
```powershell
cd C:\path\to\reconx
# or right-click the project folder → "Open in Terminal"
```

> All commands from Step 2 onward assume you are **inside the project root folder**.

---

### Step 2 — Clean slate (remove any previous containers and volumes)

**Mac:**
```bash
docker compose down -v
```

**Windows (PowerShell):**
```powershell
docker compose down -v
```

> This deletes old containers and data volumes so the demo starts completely fresh.
> Safe to skip if this is the very first run.

---

### Step 3 — Build the backend JAR

The Dockerfile picks up the compiled JAR from `target/` — build it first.

**Mac:**
```bash
cd backend
./mvnw -q clean package -DskipTests
cd ..
```

**Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd -q clean package "-DskipTests"
cd ..
```

> Expected output: a line like `BUILD SUCCESS`. This takes 1–3 minutes on first run
> (Maven downloads dependencies); subsequent runs are much faster.

---

### Step 4 — Start the full 7-service stack

**Mac:**
```bash
docker compose up -d --build
```

**Windows (PowerShell):**
```powershell
docker compose up -d --build
```

> Docker builds the backend and frontend images, then starts all 7 containers in the background.
> First build takes 3–5 minutes. Subsequent runs take ~30 seconds.

---

### Step 5 — Wait for every service to be healthy

**Mac:**
```bash
# Refreshes every 2 seconds — press Ctrl+C to stop watching once all are healthy
watch -n 2 'docker compose ps'
```

**Windows (PowerShell):**
```powershell
# Prints status every 3 seconds — press Ctrl+C to stop
while ($true) { docker compose ps; Start-Sleep 3; Clear-Host }
```

Wait until the table shows all services as running or healthy:

| Container | Expected status |
|-----------|----------------|
| reconx-postgres | `healthy` |
| reconx-zookeeper | `running` |
| reconx-kafka | `healthy` |
| reconx-backend | `healthy` |
| reconx-frontend | `running` |
| reconx-prometheus | `running` |
| reconx-grafana | `running` |

> The backend takes the longest (~45s) because it waits for Postgres **and** Kafka to be healthy first.

---

### Step 6 — Confirm the backend is responding

**Mac:**
```bash
curl http://localhost:8081/api/actuator/health
# Expected: {"status":"UP", ...}
```

**Windows (PowerShell):**
```powershell
Invoke-RestMethod http://localhost:8081/api/actuator/health
# Expected: status UP
```

---

### Step 7 — Open all demo tabs in the browser

Open these four tabs **before** you start talking through the demo:

| # | URL | What to show |
|---|-----|--------------|
| 1 | http://localhost:5173 | Frontend SPA — trade dashboard |
| 2 | http://localhost:8081/api/swagger-ui.html | Swagger UI — REST API docs |
| 3 | http://localhost:3000 | Grafana — ReconX Overview dashboard |
| 4 | http://localhost:9090/targets | Prometheus — confirm backend target is UP |

Grafana login: **admin / admin** (no setup needed — provisioned automatically).

---

### Step 8 — Login and create trades via the SPA

1. Go to **http://localhost:5173**
2. Login with username `admin`, password `admin`
3. Navigate to the **Trades** page
4. Use the **Create Trade** form to add 3 trades — fill in any values you like
5. Each trade appears in the list **without a page refresh** (Server-Sent Events push)
6. Point out the **live feed panel** updating in real time

> This demonstrates the full path: SPA → REST API → Kafka → reconciliation engine → SSE push back to browser.

---

### Step 9 — Show the REST API via Swagger

1. Go to **http://localhost:8081/api/swagger-ui.html**
2. Expand **Auth** → `POST /auth/login`
   - Click **Try it out** → body: `{ "username": "admin", "password": "admin" }` → **Execute**
   - Copy the `token` value from the response
3. Click **Authorize** (top-right lock icon) → paste the token → **Authorize** → **Close**
4. Expand **Trades** → `GET /trades` → **Execute** — shows the 3 trades created via the SPA
5. Expand **Audit** → `GET /audit/trades/{id}/history`
   - Paste one of the trade IDs → **Execute** — shows the full revision history (Hibernate Envers)

---

### Step 10 — Show Kafka topics via Kafdrop (optional)

Start Kafdrop with the debug profile:

**Mac:**
```bash
docker compose --profile debug up -d kafdrop
```

**Windows (PowerShell):**
```powershell
docker compose --profile debug up -d kafdrop
```

Open **http://localhost:9000** and show:

| Topic | What it contains |
|-------|-----------------|
| `trade-events` | Messages published when trades were created |
| `recon-results` | Reconciliation output for each trade |
| `audit-events` | Mutation events feeding the audit log |
| `trade-events-dlq` | Dead-letter queue — messages that failed 3 retries |

Click on a message to show the JSON payload structure.

---

### Step 11 — Run the k6 load test while watching Grafana

Open **http://localhost:3000** → Dashboards → **ReconX Overview** — keep it visible.

Open a **second terminal** in the project root and run:

**Mac:**
```bash
k6 run load-tests/trade-creation.js
```

**Windows (PowerShell):**
```powershell
k6 run load-tests\trade-creation.js
```

**While k6 runs (60 seconds), narrate what you see in Grafana:**
- `trade_created_total` counter climbing rapidly (~200 trades/sec)
- `http_server_requests_seconds` P95 latency staying flat
- JVM heap and CPU panels reacting to the load

**After k6 finishes, show the terminal summary:**
```
✓ trade created 201
✓ id returned

checks.........................: 100.00%
http_req_failed................: 0.00%    ← must be < 1%
http_req_duration p(95)........: 380ms    ← must be < 800ms
trades_created_total...........: 12000+
```

---

### Step 12 — Run quality gates (show CI works locally too)

**Mac:**
```bash
cd backend
./mvnw verify -Pci
open target/site/jacoco/index.html   # opens coverage report in browser
cd ..
```

**Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd verify "-Pci"
start target\site\jacoco\index.html   # opens coverage report in browser
cd ..
```

> Look for **Total line coverage ≥ 85%** in the JaCoCo report.
> If Checkstyle finds a violation the build fails with a clear error message.

---

### Step 13 — Show the CI pipeline on GitHub

1. Open your GitHub repository in the browser
2. Click the **Actions** tab
3. Click the latest workflow run on `main`
4. Walk through the 3 jobs:
   - **Liquibase validate** — changelog is consistent ✓
   - **Maven verify** — tests pass, JaCoCo ≥ 85%, Checkstyle clean ✓
   - **Docker build & push** — images pushed to GHCR ✓

---

### Step 14 — Tag the v1.0.0 release

Run this **after** CI is green on `main`:

**Mac:**
```bash
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
git push origin v1.0.0
```

**Windows (PowerShell):**
```powershell
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
git push origin v1.0.0
```

> This triggers CI to build versioned images:
> `ghcr.io/<org>/reconx-backend:1.0.0` and `ghcr.io/<org>/reconx-frontend:1.0.0`

---

### Step 15 — Tear down

**Mac:**
```bash
# If Kafdrop was started:
docker compose --profile debug down -v

# If Kafdrop was NOT started:
docker compose down -v
```

**Windows (PowerShell):**
```powershell
# If Kafdrop was started:
docker compose --profile debug down -v

# If Kafdrop was NOT started:
docker compose down -v
```

> The `-v` flag removes all data volumes — Postgres data, Grafana state, etc.
> Omit `-v` if you want to keep the data for a second demo run.

---

### Quick Reference — all URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend SPA | http://localhost:5173 | admin / admin |
| Backend Swagger | http://localhost:8081/api/swagger-ui.html | — (use JWT from /auth/login) |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Kafdrop (debug) | http://localhost:9000 | — |

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Backend container exits — `IllegalArgumentException: JWT_SECRET` | Set `JWT_SECRET` in `docker-compose.yml` under `backend.environment` |
| Prometheus panels show "No data" | Confirm `monitoring/prometheus/prometheus.yml` target is `backend:8081`, not `localhost` |
| Grafana dashboards empty | Datasource URL must use `http://prometheus:9090` (service name), not `localhost` |
| JaCoCo below 85% | Add tests for exception paths and empty-result branches |
| k6 reports lots of 401 | Token expires mid-run — call `/auth/login` inside k6 `setup()` (already done in the provided script) |
| `docker compose up` hangs | Run `docker compose logs <service>` for the stuck service |
