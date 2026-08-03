# TICKET-ADV145 — Kafka consumer config review: findings

Reviewed against the actual `spring.kafka` block in
`backend/src/main/resources/application.yml` and
`backend/src/main/java/com/dbtraining/reconx/kafka/KafkaErrorHandlerConfig.java`
as of the Day 9 Wave 1 PRs (ADV128, ADV134, ADV135, ADV139).

**Decision column is intentionally left blank** — per the ticket, the
decision (and its rationale) has to be the team's, not the reviewer's. At
least one Accept, one Reject, and one Defer should end up recorded here
before this PR is considered done.

| # | Area | Finding | Recommendation | Decision | Rationale |
|---|------|---------|-----------------|----------|-----------|
| 1 | Backpressure | No `max.poll.records` override — defaults to 500. `AuditEventConsumer` does a DB write per record; a 500-record batch that's individually slow risks tripping `max.poll.interval.ms` and triggering a rebalance mid-batch. | Set `spring.kafka.consumer.properties.max.poll.records: 100` | | |
| 2 | Backpressure | No `concurrency` set on the listener container factory. `trade-events` has 3 partitions but each of the three consumer groups (`recon-service`, `audit-service`, `alert-service`) runs single-threaded by default — one partition is doing all the work per group. | Set `factory.setConcurrency(3)` on the trade-events container factory (or `spring.kafka.listener.concurrency: 3`) | | |
| 3 | Error handling / DLQ | `ExponentialBackOff` has no jitter. At scale, many partitions failing at once (e.g. a downstream DB outage) retry in lockstep and hit the recovered dependency simultaneously — a thundering-herd risk. | Wrap in a jittered backoff, or accept as a documented gap | | |
| 4 | Error handling / DLQ | Only `DeserializationException` is on the not-retryable list. A malformed *business* payload (e.g. missing `tradeRef`) that throws `IllegalArgumentException` in a listener still burns the full 3-retry budget before DLQ, even though retrying won't fix a permanently-bad payload. | Add `IllegalArgumentException` (and any other clearly non-transient exception types your listeners throw) to `addNotRetryableExceptions(...)` | | |
| 5 | Idempotence | Producer does not explicitly set `enable.idempotence: true`. Kafka 3.0+ defaults this to true when `acks=all` (also a default since 3.0), but neither is asserted in config — a future Kafka client upgrade or explicit `acks` override could silently disable it. | Explicitly set `spring.kafka.producer.properties.enable.idempotence: true` and `acks: all` so the guarantee doesn't depend on client-version defaults | | |
| 6 | Idempotence (positive finding) | `AuditLogEntry.eventId` already has a DB-level `UNIQUE` constraint, so even under at-least-once delivery (retries, rebalances), a duplicate `TradeEvent` can't produce a duplicate audit row — the insert throws instead. | No action needed; call this out in the PR so the next engineer doesn't assume it's missing | | |
| 7 | Observability | The Day-5 MDC `correlationId`/`tradeRef` logging pattern (`logging.pattern.correlation`) is wired for the HTTP request thread but there's no equivalent propagation into Kafka listener threads — a log line from `AuditEventConsumer` has no correlation ID tying it back to the original HTTP request that created the trade. | Add a `RecordInterceptor` (or manual MDC.put in each listener) that copies `eventId`/`tradeRef` from the record into MDC before invoking the listener | | |
| 8 | Security | `spring.kafka.bootstrap-servers` is PLAINTEXT (`localhost:9092` in dev, whatever `KAFKA_BOOTSTRAP` resolves to in other envs) with no SASL/TLS configured anywhere in the Kafka block. | Use `SASL_SSL` + `security.protocol` in any non-dev profile; PLAINTEXT is acceptable for local dev only | | |

## Areas reviewed with no findings

- **Topic partitioning** (ADV128): partition counts match their stated
  ordering/parallelism goals (3 for `trade-events`/DLQ, 2 for
  `recon-results`, 1 for strictly-ordered `system-alerts`) — no issue.
- **Metrics** (ADV139): both the Boot-side binder flag and the Kafka-client
  `metric.reporters` property are set, and `application` tag is applied —
  the two-places-to-enable trap the ticket warns about is already avoided.
