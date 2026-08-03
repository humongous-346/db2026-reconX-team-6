package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV143 — end-to-end happy path: publish 100 TradeEvents, confirm
 * AuditEventConsumer (ADV132) persists exactly 100 new audit_log rows,
 * against a real Testcontainers-backed Kafka broker.
 *
 * NOTE: this test could not be executed in the sandbox this was written in
 * — Testcontainers needs direct Docker Engine API access, which that
 * environment doesn't expose (only the `docker` CLI works there). Verified
 * by compilation + code review only; run this once on a machine/CI with
 * real Docker access before relying on it.
 */
@Testcontainers
@SpringBootTest
class KafkaPipelineIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired TradeEventProducer producer;
    @Autowired AuditLogRepository auditRepo;

    @Test
    void publishesAndConsumes100Events() {
        long before = auditRepo.count();

        IntStream.range(0, 100).forEach(i ->
                producer.publish(new TradeEvent(UUID.randomUUID(), "TRD-IT-" + i,
                        TradeEvent.EventType.TRADE_CREATED, Instant.now(), "it@db.com",
                        null, "{\"price\":" + i + "}"))
        );

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(auditRepo.count()).isEqualTo(before + 100)
                );
    }
}
