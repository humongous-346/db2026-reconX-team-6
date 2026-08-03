package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/** TICKET-ADV134 — DLQ destination resolver + not-retryable wiring. */
@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    @Mock private KafkaTemplate<Object, Object> template;

    @Test
    void resolveDlqDestination_appendsDlqSuffixAndKeepsPartition() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("trade-events", 2, 10L, "key", "value");

        TopicPartition destination = KafkaErrorHandlerConfig.resolveDlqDestination(
                record, new RuntimeException("boom"));

        assertThat(destination.topic()).isEqualTo("trade-events-dlq");
        assertThat(destination.partition()).isEqualTo(2);
    }

    @Test
    void resolveDlqDestination_preservesPartitionAcrossDifferentTopics() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("recon-results", 0, 5L, "key", "value");

        TopicPartition destination = KafkaErrorHandlerConfig.resolveDlqDestination(
                record, new IllegalStateException("bad state"));

        assertThat(destination.topic()).isEqualTo("recon-results-dlq");
        assertThat(destination.partition()).isEqualTo(0);
    }

    @Test
    void errorHandler_beanIsConstructedSuccessfully() {
        lenient().when(template.getDefaultTopic()).thenReturn(null);
        KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

        DefaultErrorHandler handler = config.errorHandler(template);

        assertThat(handler).isNotNull();
    }
}
