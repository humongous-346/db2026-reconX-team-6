# ReconX — Demo Runsheet (ADV163)
# Target: 20 minutes | Run this at least twice before the real demo

---

## Before You Start (T-30 min)

- [ ] Stack is **stopped** and volumes are clean: `docker compose down -v`
- [ ] Repo is on the `main` branch and CI is green
- [ ] Docker Desktop is running with ≥ 4 GB RAM
- [ ] Browser tabs pre-opened (hidden): SPA, Swagger, Grafana, Kafdrop (debug)
- [ ] Terminal with project root as CWD
- [ ] Backup screenshots loaded in a separate browser window (`screenshots/`)

---

## Segment 1 — Stack Startup (2 min)

**Say:** "ReconX ships as a fully containerised stack — one command brings up all 7 services."

```bash
# Terminal 1
docker compose up -d --build
```

While building, narrate what's happening:
- Maven compiles the backend JAR (multi-stage Dockerfile)
- npm builds the React SPA → nginx image
- Postgres, Kafka, Prometheus, Grafana start in parallel

```bash
# Show health coming green
watch 'docker compose ps'
```

**Wait for:** all services show `(healthy)`.

**Say:** "Every service has a healthcheck. The backend only starts after Postgres and Kafka are both healthy — no race conditions."

---

## Segment 2 — Backend API via Swagger (3 min)

Open: **http://localhost:8081/api/swagger-ui.html**

**Say:** "Every endpoint is documented via OpenAPI. Let me walk through the main flows."

1. Expand **Auth** → `POST /auth/login`
   - Body: `{ "username": "admin", "password": "admin" }`
   - Click **Execute** → copy the `token`
   - Click **Authorize** (top right) → paste token → **Authorize**

2. Expand **Trades** → `POST /trades`
   ```json
   {
     "tradeRef": "DEMO-001",
     "instrument": "AAPL",
     "direction": "BUY",
     "quantity": 500,
     "price": 175.00,
     "tradeDate": "2025-01-10",
     "settlementDate": "2025-01-13",
     "counterparty": "CPTY-A",
     "currency": "USD"
   }
   ```
   - Note the `id` in the response

3. `GET /trades/{id}` → show the trade is persisted

**Say:** "Each trade creation publishes a Kafka event — the reconciliation engine picks it up asynchronously."

---

## Segment 3 — Frontend SPA (3 min)

Open: **http://localhost:5173**

1. Login with `admin / admin`
2. Show the **Trades** list — DEMO-001 appears
3. Create two more trades via the UI form
4. Show the **live feed** (SSE stream) — new trades appear without a page refresh

**Say:** "The trade list uses Server-Sent Events — the backend pushes updates over a persistent HTTP connection. No polling."

---

## Segment 4 — Kafka Pipeline (2 min)

Open: **http://localhost:9000** (start Kafdrop first if needed)

```bash
# Terminal 1
docker compose --profile debug up -d kafdrop
```

Show:
- **trade-events** topic — messages from our 3 trades
- **recon-results** topic — reconciliation output for each trade
- **audit-events** topic — mutation events for the audit log
- Click a message → show the JSON payload

**Say:** "The DLQ consumer catches any message that fails 3 retries. Ops can inspect and replay messages without touching the main pipeline."

---

## Segment 5 — Audit Trail (2 min)

Back in Swagger:

1. `GET /api/audit/trades/{id}/history` — show the full revision history
2. Update the trade (e.g., change quantity) → run history again
3. Show two revisions: CREATE and UPDATE, with timestamps

**Say:** "Hibernate Envers writes to `_AUD` tables automatically — no triggers, no CDC, no custom code."

---

## Segment 6 — Observability (4 min)

Open: **http://localhost:3000** (Grafana — admin/admin)

Navigate: **Dashboards → ReconX Overview**

Show live panels:
- `trade_created_total` — counter climbing as trades were posted
- `reconciliation_duration_seconds` — P95 recon latency (should be <10ms)
- `http_server_requests_seconds` — API latency by endpoint

Now open a second terminal and run the k6 load test:

```bash
# Terminal 2
k6 run load-tests/trade-creation.js
```

**While k6 runs (60s):** narrate what you see in Grafana:
- Trade counter climbing rapidly (~200 req/s)
- JVM heap and CPU panels reacting
- No latency spike — the cache + async Kafka consumption absorbs load

After k6 finishes:
- Show the summary: `http_req_failed < 1%`, `p(95) < 800ms`

---

## Segment 7 — CI & Release (2 min)

Screen-share GitHub Actions (or show a saved screenshot):

1. Show the CI workflow on a recent push to `main`
2. Point out the 4 jobs: Liquibase validate → build+test → JaCoCo report → Docker push
3. Show the GHCR package: `ghcr.io/<org>/reconx-backend:sha-...`

**Say:** "When we tag `v1.0.0`, CI builds an image with the version tag and pushes it to GHCR — ready for Kubernetes or any container runtime."

```bash
# (optional live) show the tag command
git tag -a v1.0.0 -m "Release 1.0.0 — ReconX Advanced Track"
# (don't push live — it triggers CI)
```

---

## Segment 8 — Q&A Buffer (2 min)

Common questions and prepared answers:

**Q: How does the reconciliation engine know what's a break?**
A: It compares the incoming trade event against the expected position in the PMS reference.
   Any delta beyond the configurable tolerance threshold is a break.

**Q: What happens when Kafka is temporarily down?**
A: Kafka has a healthcheck gate in compose. In production you'd run a 3-broker cluster with replication.
   The DLQ catches transient failures.

**Q: How do you handle schema evolution?**
A: Liquibase changesets — each migration is in a separate file, checksummed and run exactly once on startup.
   Breaking changes are backwards-compatible (add nullable columns, never drop used columns).

---

## Teardown

```bash
docker compose down -v    # removes containers + volumes
docker compose --profile debug down -v   # if kafdrop was started
```

---

## Contingency — If Live Demo Breaks

| Issue | Fallback |
|-------|----------|
| `docker compose up` fails | Show `screenshots/01-stack-healthy.png` |
| SPA won't load | Use Swagger UI directly |
| Grafana empty panels | Show `screenshots/02-grafana-under-load.png` |
| k6 fails to start | Show `screenshots/03-k6-results.png` |
| Kafka hangs | Re-run `docker compose restart kafka zookeeper` |
