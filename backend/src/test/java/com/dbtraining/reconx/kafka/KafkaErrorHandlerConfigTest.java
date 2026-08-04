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

/**
 * TICKET-ADV134 — DLQ destination resolver + not-retryable wiring.
 * TICKET-ADV135 — exponential backoff timing (~1s, ~2s, ~4s, then DLQ).
 */
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

    @Test
    void retryBackOff_initialIntervalAndMultiplierMatchSpec() {
        ExponentialBackOff backoff = KafkaErrorHandlerConfig.retryBackOff();

        assertThat(backoff.getInitialInterval()).isEqualTo(1000L);
        assertThat(backoff.getMultiplier()).isEqualTo(2.0);
        // 7000ms (not the 8000ms figure quoted loosely elsewhere) is what
        // actually stops after exactly three retries — see the Javadoc on
        // retryBackOff() for the measured reasoning.
        assertThat(backoff.getMaxElapsedTime()).isEqualTo(7_000L);
    }

    @Test
    void retryBackOff_producesRoughlyThreeRetriesBeforeStopping() {
        ExponentialBackOff backoff = KafkaErrorHandlerConfig.retryBackOff();
        BackOffExecution execution = backoff.start();

        // t=0 -> first retry waits ~1s
        assertThat(execution.nextBackOff()).isEqualTo(1000L);
        // cumulative ~1s -> second retry waits ~2s
        assertThat(execution.nextBackOff()).isEqualTo(2000L);
        // cumulative ~3s -> third retry waits ~4s
        assertThat(execution.nextBackOff()).isEqualTo(4000L);
        // cumulative 7s == maxElapsedTime(7000) -> BackOffExecution signals STOP.
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }
}
