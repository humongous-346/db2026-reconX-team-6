# ReconX — Sprint Retrospective (ADV165)

**Project:** ReconX Advanced Track (DB26)
**Period:** Day 1 – Day 10
**Date:** [Fill in after demo]
**Participants:** [Fill in team / individual]

---

## 1. What Went Well

### Architecture & Design
- The C4 modelling on Day 1 paid off throughout the sprint — having a clear container
  diagram meant every new service fit into a known slot without debating placement.
- Kafka as the event bus made the reconciliation engine easy to test in isolation
  (produce a message, assert the database state — no HTTP layer involved).
- Hibernate Envers gave us a full audit trail with zero custom SQL. It just worked.

### Testing & Quality
- Testcontainers integration tests caught a real Liquibase migration bug that a mocked
  PostgreSQL would never have surfaced.
- The JaCoCo 85% gate forced us to write tests for exception paths we had initially
  skipped. Two of those paths had real bugs.
- Checkstyle kept the codebase consistent, especially useful when switching between
  controller and Kafka layers.

### Observability
- Micrometer + Prometheus + Grafana provisioning-as-code meant Grafana dashboards
  appeared on first `docker compose up` — no manual panel creation.
- The Prometheus alert rules for DLQ growth caught a Kafka serialisation issue during
  development before it would have hit users in production.

### Developer Experience
- Multi-stage Dockerfiles reduced the final backend image size significantly
  (full JDK → slim JRE).
- `docker compose up -d --build` being the single entrypoint for the full stack
  eliminated "works on my machine" issues.

---

## 2. What Could Have Been Better

### Things That Slowed Us Down
- **Kafka port confusion:** The internal broker listener (`:29092`) vs the host-exposed
  listener (`:9092`) caused early integration test failures. Should have read the
  Confluent networking docs more carefully upfront.
- **JWT expiry during load tests:** The first k6 run failed with lots of 401s because
  the token expired mid-run. Fixed by moving auth into k6's `setup()` function.
- **JaCoCo coverage gate vs generated code:** The coverage gate initially included
  MapStruct-generated mapper classes (which have no branches to test), pulling the
  number down below 85%. Added exclusions to the JaCoCo config.
- **Grafana datasource URL:** Used `localhost` instead of the service name `prometheus`
  in the datasource YAML — dashboards were empty until this was caught.

### Design Decisions We'd Revisit
- **Single Maven module:** Keeping everything in one module was convenient for the
  training, but in a real project the domain, service, and API layers should be
  separate Maven modules to enforce dependency direction.
- **No schema registry:** Kafka message contracts are embedded as trusted packages in
  `application.yml`. A schema registry (Confluent or Apicurio) would prevent
  incompatible consumer/producer versions from silently breaking each other.
- **Hard-coded admin seed user:** The dev profile seeds an `admin` user via Liquibase.
  In production this should come from an IdP (Keycloak, Okta) not a changelog.

---

## 3. What We'd Change Next Time

| Area | Change |
|------|--------|
| CI | Add k6 as a CI job gated on p95 < SLA threshold |
| Kafka | Start with multi-partition topics and consumer group scaling from Day 1 |
| Security | Wire JWT refresh token rotation — current expiry is per-login only |
| Observability | Add distributed tracing (OpenTelemetry → Grafana Tempo) |
| Docs | Write the architecture diagrams and ADRs on Day 1, not Day 10 |
| Testing | Add contract tests (Pact) between the SPA and the backend API |
| Deployment | Write a Helm chart in parallel with the Dockerfiles |

---

## 4. Key Technical Learnings

1. **Kafka healthcheck timing** — `kafka-topics --list` is the most reliable healthcheck
   for Kafka in Docker Compose (others like nc or curl-based checks fire before the
   broker is ready to serve requests).

2. **`depends_on: condition: service_healthy`** is critical; without it the backend
   starts while Kafka is still initialising and the first consumer registration fails.

3. **Testcontainers `@DynamicPropertySource`** is the cleanest way to wire the container
   ports into Spring's environment — no manual property overrides needed.

4. **JaCoCo `prepare-agent` must come before the Surefire plugin runs** — if the
   `prepare-agent` execution is missing, coverage is always 0%.

5. **Checkstyle runs at `validate` phase** — this means it runs before `compile`,
   catching style errors before wasting time on a full build.

6. **Grafana provisioning from files** — `GF_PATHS_PROVISIONING` + datasource and
   dashboard YAML files in a mounted volume means zero manual Grafana setup across
   every environment.

---

## 5. What We're Proud Of

- A real, working, production-grade trade reconciliation system built from scratch in 10 days.
- Full observability: every key metric, alert, and dashboard in place from Day 1.
- A CI pipeline that enforces coverage, style, and Liquibase consistency on every commit.
- A one-command demo that works cold, without any pre-built state.
