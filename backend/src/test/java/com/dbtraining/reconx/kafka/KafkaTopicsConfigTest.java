package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV128 — the four NewTopic beans have the correct name/partitions/replicas. */
class KafkaTopicsConfigTest {

    private final KafkaTopicsConfig config = new KafkaTopicsConfig();

    @Test
    void tradeEvents_has3PartitionsReplicas1() {
        NewTopic topic = config.tradeEvents();
        assertThat(topic.name()).isEqualTo("trade-events");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void tradeEventsDlq_matchesMainTopicPartitionCount() {
        NewTopic topic = config.tradeEventsDlq();
        assertThat(topic.name()).isEqualTo("trade-events-dlq");
        // Must equal trade-events' partition count so the DLQ recoverer can
        // preserve the original partition number.
        assertThat(topic.numPartitions()).isEqualTo(config.tradeEvents().numPartitions());
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void reconResults_has2Partitions() {
        NewTopic topic = config.reconResults();
        assertThat(topic.name()).isEqualTo("recon-results");
        assertThat(topic.numPartitions()).isEqualTo(2);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void systemAlerts_has1PartitionForStrictOrdering() {
        NewTopic topic = config.systemAlerts();
        assertThat(topic.name()).isEqualTo("system-alerts");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void constantsMatchBeanNames() {
        assertThat(KafkaTopicsConfig.TRADE_EVENTS).isEqualTo(config.tradeEvents().name());
        assertThat(KafkaTopicsConfig.TRADE_EVENTS_DLQ).isEqualTo(config.tradeEventsDlq().name());
        assertThat(KafkaTopicsConfig.RECON_RESULTS).isEqualTo(config.reconResults().name());
        assertThat(KafkaTopicsConfig.SYSTEM_ALERTS).isEqualTo(config.systemAlerts().name());
    }
}
