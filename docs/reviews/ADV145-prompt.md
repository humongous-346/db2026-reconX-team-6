# TICKET-ADV145 — Kafka consumer config review prompt

The prompt sent for this review, verbatim:

```text
Review the following Spring Kafka consumer configuration for production
readiness. Flag any missing or risky settings in these areas:
  (1) backpressure & poll tuning,
  (2) error handling, retry & DLQ,
  (3) idempotence and exactly-once semantics,
  (4) observability — metrics, logging, traces,
  (5) security — TLS, SASL, ACLs.

For each finding, give the concrete config key, the recommended value, and a
one-line justification. Do NOT rewrite the whole file — just list findings.

Application context: trade reconciliation service, ~500 events/sec, strict
audit requirements.

=== application.yml (spring.kafka section) ===
<paste the current backend/src/main/resources/application.yml
 spring.kafka block>

=== KafkaErrorHandlerConfig.java ===
<paste backend/src/main/java/com/dbtraining/reconx/kafka/KafkaErrorHandlerConfig.java>
```

See `ADV145-findings.md` in this folder for the findings returned and the
team's accept/reject/defer decisions.
