```mermaid
C4Container
  title C4 Container - ReconX

  %% External Actors
  Person(user, "User", "Trader / Analyst / Admin")
  System_Ext(oms, "Internal OMS", "Upstream trade source")
  System_Ext(sso, "Corporate SSO", "OIDC IdP")

  %% System Boundary
  System_Boundary(reconxBoundary, "ReconX") {
    Container(spa, "Recon UI", "React 19 + Vite", "Single-page app. Live trade feed via SSE; trades + breaks tables; admin views.")
    Container(api, "recon-service API", "Java 25 + Spring Boot 3", "REST API. JWT auth, RBAC, validation, exposes /actuator/prometheus.")
    Container(engine, "Reconciliation Engine", "Spring + CompletableFuture", "Async batch + streaming match logic. Writes recon_breaks.")
    ContainerDb(db, "PostgreSQL 16", "Liquibase-managed", "Partitioned trades, recon_breaks, audit_log, mat. views.")
    ContainerQueue(kafka, "Apache Kafka", "3 topics + DLQs", "trade-events, recon-results, system-alerts. DLQ per topic.")
    Container(prometheus, "Prometheus", "TSDB", "Scrapes the API every 15s.")
    Container(grafana, "Grafana", "Dashboard", "Pre-provisioned dashboards.")
  }

  %% Relationships
  Rel(user, spa, "Uses", "HTTPS")
  Rel(user, sso, "Authenticates", "OIDC")
  Rel(spa, api, "Makes API calls", "REST + SSE / HTTPS / JSON")
  Rel(oms, kafka, "Streams trades", "Kafka topic: trade-events")
  Rel(api, db, "Reads/Writes", "JDBC")
  Rel(api, kafka, "Publishes events", "Kafka")
  Rel(engine, db, "Reads/Writes trades & breaks", "JDBC")
  Rel(engine, kafka, "Consumes & Publishes events", "Kafka")
  Rel(prometheus, api, "Scrapes metrics", "HTTPS")
  Rel(grafana, prometheus, "Queries metrics", "HTTPS / PromQL")