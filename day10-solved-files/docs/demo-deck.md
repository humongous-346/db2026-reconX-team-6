# ReconX Demo Deck — ADV162
# 10 Slides for the Final Presentation

---
> **How to use this file**
> Convert to `.pptx` / `.pdf` using any Markdown → slides tool
> (Marp, Slidev, Pandoc, Google Slides import, or copy-paste into PowerPoint).
> Estimated talk time: 20 minutes.
---

## Slide 1 — Title

**ReconX**
*Enterprise Trade Reconciliation Platform*

Advanced Track — DB26
Team: [Your Name]
Date: [Presentation Date]

> **Speaker note:** Introduce yourself and give one sentence on what reconciliation is:
> "ReconX detects when what we traded doesn't match what our counterparties recorded — in real time."

---

## Slide 2 — The Problem We Solved

**Why trade reconciliation matters**

- Financial firms process millions of trades daily
- Even a 0.01% mismatch = thousands of breaks to investigate manually
- Manual reconciliation: slow, error-prone, costly
- RegTech requirement: full audit trail for every trade mutation

**What ReconX delivers:**
- Automated break detection via Kafka-driven pipeline
- Real-time alerting + Grafana dashboards
- Immutable audit trail (Hibernate Envers)
- One-command stack startup

> **Speaker note:** One slide, one idea. Don't read the bullets — expand on "what a recon break costs" instead.

---

## Slide 3 — System Architecture

*(Insert C4 Container diagram from `db/diagrams/architecture.md` — Level 2)*

**7 services, one compose file:**

| Service | Role |
|---------|------|
| React SPA | Trade dashboard + live alerts feed |
| Spring Boot API | REST + Kafka consumer + recon engine |
| PostgreSQL 16 | Trades, positions, audit log |
| Apache Kafka | Event bus — 4 topics |
| Prometheus | Metrics scrape every 10s |
| Grafana | Dashboards + alert rules |

> **Speaker note:** Walk through the flow — trader submits a trade → REST → Kafka → recon engine → break alert → Grafana.

---

## Slide 4 — Tech Stack Decisions

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Java 21 (LTS) | Virtual threads, records, sealed types |
| Framework | Spring Boot 3.5 | Production-grade; team familiarity |
| Messaging | Apache Kafka | Durable log; enables replay; DLQ pattern |
| DB | PostgreSQL 16 + Liquibase | JSONB, ACID, schema-versioned migrations |
| Auth | JWT (jjwt 0.12) | Stateless; scales horizontally |
| Cache | Caffeine | In-process; sub-millisecond; no Redis ops overhead |
| Metrics | Micrometer → Prometheus | Zero overhead; plug-in via Actuator |
| Frontend | React + Vite | Fast HMR in dev; tiny prod bundle |
| Tests | Testcontainers | Real Postgres + Kafka in CI; no mock divergence |

---

## Slide 5 — Key Feature: Kafka Pipeline

*(Insert diagram or screenshot of Kafdrop showing 4 topics)*

```
Trade created
      │
      ▼
trade-events ──► ReconciliationConsumer ──► recon-results
                        │
                        ▼ (on error)
               trade-events-dlq ──► DlqAdminController (replay / discard)
                        │
                        ▼ (on break)
               recon-alerts ──► AlertConsumer ──► SSE → SPA
```

**Dead-letter queue:** poison-pill messages never block the main pipeline.
DLQ messages are inspectable and replayable via `/api/dlq-admin` endpoints.

---

## Slide 6 — Key Feature: Audit Trail

**Hibernate Envers — zero-SQL, immutable history**

- Every `@Audited` entity gets a `_AUD` table automatically
- Every INSERT / UPDATE / DELETE is versioned with a revision number and timestamp
- `GET /api/audit/trades/{id}/history` returns the full change log
- Survives application restarts and schema migrations

*(Show screenshot of Swagger UI hitting the audit endpoint)*

---

## Slide 7 — Observability

*(Insert Grafana dashboard screenshot — ReconX Overview panel)*

**What we track:**

| Metric | Meaning |
|--------|---------|
| `trade_created_total` | Trades processed — rate under load |
| `recon_break_count` | Open breaks — alert fires if > 50 for 5 min |
| `reconciliation_duration_seconds` | P95 recon latency |
| `http_server_requests_seconds` | API latency by endpoint |
| `kafka_consumer_lag` | Consumer falling behind? |

**Alerts wired in `alerts.yml`:**
- Too many recon breaks → warning
- P95 API latency > 500ms → warning
- DLQ growing → critical

---

## Slide 8 — Quality Gates & CI

*(Insert GitHub Actions run screenshot)*

```
push to main
     │
     ▼
┌─────────────────────────────┐
│  1. Liquibase validate       │
│  2. mvn verify -Pci          │
│     └─ Tests (Testcontainers)│
│     └─ JaCoCo ≥ 85% lines   │
│     └─ Checkstyle (Google)   │
│  3. Docker build & push      │
│     └─ backend:sha-abc123    │
│     └─ frontend:sha-abc123   │
└─────────────────────────────┘
     │
     ▼ (on v* tag)
  ghcr.io/<org>/reconx-backend:1.0.0
  ghcr.io/<org>/reconx-frontend:1.0.0
```

**k6 load test:** 200 VUs × 60s → p(95) < 800ms, error rate < 1%

---

## Slide 9 — Live Demo

**Demo path (follow `docs/demo-runsheet.md`):**

1. `docker compose up -d --build` — watch all 7 services go green
2. Open SPA at `localhost:5173` — login as admin
3. Create 3 trades via UI → see them appear in the trade list
4. Open Grafana → show `trade_created_total` climbing
5. Trigger a recon run → show a break alert on the SPA
6. Inspect the DLQ admin page
7. Show the audit history for one trade

*(Screenshots in `screenshots/` as backup if live demo has issues)*

---

## Slide 10 — Retrospective Highlights & Next Steps

**What worked well:**
- Testcontainers eliminated environment drift — tests pass everywhere
- Kafka DLQ kept the pipeline resilient during development
- Docker Compose made onboarding a single command

**What we'd do differently:**
- Add a schema registry (Confluent) to enforce Kafka message contracts
- Split into Maven modules earlier (api / service / domain)
- Automate k6 as a CI job rather than a manual step

**Next steps for production:**
- Kubernetes deployment (Helm chart)
- TLS everywhere (cert-manager)
- Multi-partition Kafka with consumer group scaling
- Distributed tracing (OpenTelemetry → Tempo)

---

*Thank you — questions?*
