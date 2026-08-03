# Day 6 — Grafana Panels Reference (ADV087–ADV094)

> No Java code required. These are PromQL queries configured inside the Grafana UI.
> Requires Docker Compose running with Prometheus and Grafana.

---

## Prerequisites — Start the Stack

```bash
# From the project root
docker compose up -d postgres prometheus grafana

# Start the backend on uat profile (so Prometheus can scrape it)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| App metrics | http://localhost:8081/api/actuator/prometheus | — |

### Verify Prometheus is scraping the app
In Prometheus UI → **Status → Targets** → look for `recon-service` with state **UP**.

Or run:
```promql
up{job="recon-service"}
```
Should return `1`.

---

## The Observability Pipeline

```
Spring Boot app
  → /actuator/prometheus   (exposes metrics as text every scrape)
      ↓ scraped every 15s
  Prometheus               (stores time series, evaluates alert rules)
      ↓ queried via PromQL
  Grafana                  (renders panels, fires alerts)
```

**If a panel shows "No data", walk this chain backwards:**
1. Does `/actuator/prometheus` list the metric?
2. Does Prometheus show it under Graph → Execute?
3. Is the scrape target UP?
4. Is the Grafana datasource UID correct?

---

## How to Create a Panel in Grafana

1. Open Grafana → **Dashboards → New Dashboard → Add visualization**
2. Select datasource: **Prometheus**
3. Paste the PromQL query into the query field
4. Set panel type, unit, and legend as specified below
5. Click **Apply** → **Save dashboard**

---

## ADV087 — API Request Rate by Endpoint

Shows how many requests per second each endpoint is receiving. Spikes reveal hot endpoints.

**Panel type:** Time series
**Unit:** `reqps` (requests/sec)
**Legend:** `{{uri}}`

```promql
sum(rate(http_server_requests_seconds_count[1m])) by (uri)
```

**What to look for:**
- `/api/v1/trades` should spike during load tests
- `/api/actuator/prometheus` shows the Prometheus scrape itself (every 15s)

---

## ADV088 — API Response Time P50 / P95 / P99

Shows latency percentiles across all endpoints. Helps identify slow requests.

**Panel type:** Time series
**Unit:** `s` (Grafana auto-formats as ms)
**Add 3 separate queries with legends P50, P95, P99:**

```promql
# Query A — P50 (median)
histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# Query B — P95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# Query C — P99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
```

**What to look for:**
- P95 should stay below 200ms under normal load
- A spike in P99 but not P50 means only a few slow outliers

---

## ADV089 — Trade Creation Rate

Shows trades being created per second. Spikes during load tests, flat when idle.

**Panel type:** Time series
**Unit:** Custom — type `trades/s`

```promql
sum(rate(trade_created_total[1m]))
```

**What to look for:**
- Flat at 0 → no trades being created (expected when idle)
- Spike → active trading or load test running
- Counter only goes up — use `rate()` to get per-second velocity

---

## ADV090 — Trade Value Distribution (Heatmap)

Shows the distribution of trade notional sizes over time. Dark bands = common sizes.

**Panel type:** Heatmap
**Data format:** Time series buckets

```promql
sum(rate(trade_value_total_bucket[5m])) by (le)
```

**What to look for:**
- Most trades clustered in a certain value band (e.g. $10k–$100k)
- Outlier high-value trades appear as sparse dots at top

---

## ADV091 — Open Recon Breaks (Stat Tile)

A single big number showing how many reconciliation breaks are currently open.
Updates live on every Prometheus scrape (every 15s).

**Panel type:** Stat
**Unit:** `short` (count)
**Thresholds:** Green = 0, Yellow = 5, Red = 10

```promql
recon_break_count
```

**What to look for:**
- Should be 0 when everything is reconciled
- Rises when trades mismatch during recon runs
- Falls when breaks are resolved via `PUT /v1/recon/results/{id}/resolve`

> This is a **Gauge** — it reads live from the database on every scrape via
> `breakRepo.countByStatus("OPEN")`. No code needed to update it.

---

## ADV092 — Reconciliation Duration Heatmap

Shows how long each reconciliation run takes. Useful for spotting slow recon jobs.

**Panel type:** Heatmap

```promql
sum(rate(reconciliation_duration_seconds_bucket[5m])) by (le)
```

**To generate data:** trigger a recon run:
```bash
TOKEN=<your JWT token>
curl -X POST http://localhost:8081/api/v1/recon/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"from":"2025-01-01","to":"2025-01-31"}'
```

**What to look for:**
- Most runs finishing in <10ms (small dataset in dev)
- Use `histogram_quantile(0.95, ...)` in Prometheus to get P95

---

## ADV093 — Alert: Too Many Open Recon Breaks

Fires when more than 10 recon breaks are open for more than 2 minutes.

**Where to configure:** Prometheus alert rules file (`monitoring/alert-rules.yml`)

```yaml
groups:
  - name: reconx-alerts
    rules:
      - alert: TooManyOpenBreaks
        expr: recon_break_count > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "{{ $value }} reconciliation breaks open"
          description: "Recon break count has been above 10 for 2 minutes. Investigate at /api/v1/recon/results"
```

**To test the alert:**
1. Insert 11+ rows into `recon_breaks` with `status = 'OPEN'` via H2 console
2. Wait 2 minutes
3. Prometheus UI → **Alerts** → `TooManyOpenBreaks` shows as FIRING

---

## ADV094 — Alert: High P95 API Latency

Fires when P95 response time exceeds 500ms for more than 5 minutes.

**Where to configure:** Same `monitoring/alert-rules.yml`

```yaml
      - alert: HighP95Latency
        expr: >
          histogram_quantile(
            0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 0.5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "P95 API latency is {{ $value | humanizeDuration }} — above 500ms"
          description: "95th percentile response time has been above 500ms for 5 minutes."
```

---

## Quick PromQL Reference

| Question | PromQL |
|----------|--------|
| Total trades created | `trade_created_total` |
| Trades per second (last 1m) | `rate(trade_created_total[1m])` |
| Average trade value | `trade_value_total_sum / trade_value_total_count` |
| P95 trade value | `histogram_quantile(0.95, rate(trade_value_total_bucket[5m]))` |
| Open recon breaks | `recon_break_count` |
| Request rate per endpoint | `sum(rate(http_server_requests_seconds_count[1m])) by (uri)` |
| P95 latency | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` |
| P95 recon duration | `histogram_quantile(0.95, sum(rate(reconciliation_duration_seconds_bucket[5m])) by (le))` |
| Cache hit rate | `rate(cache_gets_total{result="hit"}[5m]) / rate(cache_gets_total[5m])` |
| JVM heap used | `jvm_memory_used_bytes{area="heap"}` |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Panel shows "No data" | Check `/actuator/prometheus` — is the metric listed? |
| Prometheus target is DOWN | Is the backend running? Is port 8081 accessible from Docker? |
| `trade_created_total` always 0 | `TradeMetrics.incrementTradeCreated()` body is empty — Day 6 stub not filled in |
| `recon_break_count` not updating | Gauge auto-updates on scrape — wait 15s and refresh |
| Alert never fires | Check `for: 2m` — condition must be true continuously for 2 min |
| `histogram_quantile` returns NaN | Missing `_bucket` series — `publishPercentileHistogram()` not set on the summary |
| Grafana datasource error | Go to **Connections → Data sources → Prometheus** → test the URL |
