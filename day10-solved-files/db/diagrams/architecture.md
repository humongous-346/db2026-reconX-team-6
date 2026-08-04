# ReconX — C4 Architecture Diagrams (ADV160)

Mermaid diagrams at the Context, Container, and Component levels.
Paste each block into [Mermaid Live Editor](https://mermaid.live) to render,
or view directly in any Markdown renderer that supports Mermaid (GitHub, VS Code, etc.).

---

## Level 1 — System Context

```mermaid
C4Context
  title System Context — ReconX Trade Reconciliation Platform

  Person(trader,     "Trader / Ops User",   "Submits trades, monitors reconciliation breaks")
  Person(admin,      "System Admin",         "Manages users, DLQ, audit trails")

  System(reconx,    "ReconX",               "Enterprise trade reconciliation platform.\nIngests trade events, reconciles positions,\nalerts on breaks.")

  System_Ext(oms,   "Order Management System (OMS)",   "Source of trade events via Kafka")
  System_Ext(pms,   "Position Management System (PMS)", "Reference for expected positions")
  System_Ext(grafana, "Grafana / Prometheus",           "Observability — dashboards & alerts")

  Rel(trader,  reconx,  "Views dashboard, submits trades",   "HTTPS / SPA")
  Rel(admin,   reconx,  "Manages system",                    "HTTPS / SPA")
  Rel(oms,     reconx,  "Publishes trade events",            "Kafka (trade-events)")
  Rel(reconx,  pms,     "Fetches expected positions",        "REST / HTTPS")
  Rel(reconx,  grafana, "Exposes metrics",                   "Prometheus scrape")
```

---

## Level 2 — Container Diagram

```mermaid
C4Container
  title Container Diagram — ReconX

  Person(user, "User", "Trader / Ops / Admin")

  Container(spa,        "Frontend SPA",        "React + Vite / nginx",       "Trade dashboard, recon UI, alerts feed")
  Container(backend,    "Backend API",          "Spring Boot 3 / Java 21",    "REST API, Kafka consumers, recon engine")
  ContainerDb(postgres, "PostgreSQL 16",        "Relational DB",              "Trades, positions, recon results, audit log")
  Container(kafka,      "Apache Kafka",         "Confluent Platform 7.6",     "trade-events, recon-results, audit-events, DLQ")
  Container(prometheus, "Prometheus",           "prom/prometheus:2.54",       "Scrapes /actuator/prometheus every 10s")
  Container(grafana,    "Grafana",              "grafana/grafana:11.2",       "Dashboards: ReconX Overview + Kafka")

  Rel(user,      spa,        "Uses",                    "HTTPS :5173")
  Rel(spa,       backend,    "Calls REST API / SSE",    "HTTP /api (proxy)")
  Rel(backend,   postgres,   "Reads / writes",          "JDBC / JPA")
  Rel(backend,   kafka,      "Produces & consumes",     "Kafka client :29092")
  Rel(prometheus,backend,    "Scrapes metrics",         "HTTP /api/actuator/prometheus")
  Rel(grafana,   prometheus, "Queries",                 "PromQL :9090")
```

---

## Level 3 — Component Diagram (Backend)

```mermaid
C4Component
  title Component Diagram — ReconX Backend (Spring Boot)

  Container_Boundary(backend, "Backend API") {

    Component(tradeCtrl,   "TradeController",          "REST @RestController",      "CRUD for trades — POST /trades, GET /trades/{id}")
    Component(reconCtrl,   "ReconController",          "REST @RestController",      "Triggers reconciliation run, GET breaks")
    Component(auditCtrl,   "AuditController",          "REST @RestController",      "Paginated audit event log")
    Component(dlqCtrl,     "DlqAdminController",       "REST @RestController",      "Replay or discard dead-letter messages")
    Component(authCtrl,    "AuthController",            "REST @RestController",      "JWT login / refresh")
    Component(streamCtrl,  "TradeStreamController",    "SSE @RestController",       "Server-Sent Events for live trade feed")

    Component(tradeSvc,    "TradeService",             "@Service",                  "Business logic, cache, Kafka publish")
    Component(reconSvc,    "ReconciliationService",    "@Service",                  "Break detection, position diffing")
    Component(auditSvc,    "AuditService",             "@Service",                  "Persists audit events")

    Component(tradeProducer,  "TradeEventProducer",    "Kafka @Component",          "Publishes TradeEvent to trade-events topic")
    Component(reconConsumer,  "ReconciliationConsumer","Kafka @KafkaListener",      "Consumes trade-events, triggers recon")
    Component(auditConsumer,  "AuditEventConsumer",    "Kafka @KafkaListener",      "Persists audit events from audit-events topic")
    Component(dlqConsumer,    "DlqConsumer",           "Kafka @KafkaListener",      "Reads trade-events-dlq, exposes for admin replay")
    Component(alertConsumer,  "AlertConsumer",         "Kafka @KafkaListener",      "Sends SSE push on recon-alerts topic")

    Component(tradeRepo,   "TradeRepository",          "Spring Data JPA",           "Queries trades table with Envers audit")
    Component(reconRepo,   "ReconRepository",          "Spring Data JPA",           "Recon results & breaks")

    Component(metrics,     "TradeMetrics",             "Micrometer @Component",     "Custom counters + histograms → /actuator/prometheus")
    Component(jwtFilter,   "JwtAuthFilter",            "Spring Security Filter",    "Validates Bearer tokens on every request")
  }

  Rel(tradeCtrl,   tradeSvc,      "delegates to")
  Rel(reconCtrl,   reconSvc,      "delegates to")
  Rel(auditCtrl,   auditSvc,      "delegates to")
  Rel(tradeSvc,    tradeProducer, "publishes via")
  Rel(tradeSvc,    tradeRepo,     "reads / writes")
  Rel(reconConsumer, reconSvc,    "triggers")
  Rel(reconSvc,    reconRepo,     "reads / writes")
  Rel(auditConsumer, auditSvc,    "feeds")
  Rel(tradeSvc,    metrics,       "records")
```
