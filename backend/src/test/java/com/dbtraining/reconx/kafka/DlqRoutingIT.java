package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV144 — proves a failing listener routes to trade-events-dlq after
 * retries are exhausted.
 *
 * DEVIATION FROM THE GUIDE: the ticket's reference solution mocks
 * ReconciliationEngine.scheduleRecon(...) to throw. This codebase's
 * ReconciliationConsumer (TICKET-ADV131) doesn't call into
 * ReconciliationEngine at all — the shipped stub only logs, matching the
 * simpler pattern used across all three consumers here (see ADV131/132/133
 * PR descriptions). There is no failure-injection seam on that path.
 * AuditEventConsumer (ADV132) has a natural one instead: mock
 * AuditLogRepository.save(...) to throw. Both consumer groups subscribe to
 * the same trade-events topic/partition, so forcing audit-service to fail
 * exercises the exact same DLQ-routing path (DeadLetterPublishingRecoverer
 * -> trade-events-dlq, same partition) the ticket is actually testing.
 *
 * NOTE: not executed in the sandbox this was written in — see KafkaPipelineIT
 * for why. Verified by compilation + code review only.
 */
@Testcontainers
@SpringBootTest
class DlqRoutingIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired TradeEventProducer producer;

    @MockBean AuditLogRepository auditLogRepository;

    @Test
    void failingConsumerRoutesToDlq() {
        Mockito.doThrow(new RuntimeException("boom"))
                .when(auditLogRepository).save(Mockito.any());

        TradeEvent event = new TradeEvent(UUID.randomUUID(), "TRD-DLQ-1",
                TradeEvent.EventType.TRADE_CREATED, Instant.now(), "it@db.com", null,
                "{\"price\":100}");
        producer.publish(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() ->
                        assertThat(dlqHas("TRD-DLQ-1")).isTrue()
                );
    }

    private boolean dlqHas(String tradeRef) {
        Properties p = new Properties();
        p.put(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(GROUP_ID_CONFIG, "dlq-assert-" + System.nanoTime());
        p.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        p.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, TradeEvent>(p)) {
            consumer.subscribe(java.util.List.of("trade-events-dlq"));
            var records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, TradeEvent> r : records) {
                if (tradeRef.equals(r.value().tradeRef())) return true;
            }
        }
        return false;
    }
}
