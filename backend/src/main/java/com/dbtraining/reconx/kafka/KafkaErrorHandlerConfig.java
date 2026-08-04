package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * ============================================================================
 * TICKET-ADV134 — DLQ via DeadLetterPublishingRecoverer (failed messages
 *                routed to {topic}-dlq with the same partition number)
 * TICKET-ADV135 — Retry strategy: 3 attempts with exponential backoff
 *                (1s, 2s, 4s) before giving up to DLQ
 *
 * WHAT:    Spring Kafka error handler that retries with backoff and on
 *          final failure publishes the poison record to the corresponding
 *          DLQ topic.
 * HOW:     One @Bean DefaultErrorHandler combining a
 *          DeadLetterPublishingRecoverer + ExponentialBackOff.
 * WHY:     Without this, an exception in a listener kills the consumer
 *          thread and the whole partition stalls. With it, retries happen,
 *          and a final failure is observable (DLQ topic) rather than lost.
 * OBSERVE: Force an exception in a consumer — Kafdrop should show the
 *          record on `trade-events-dlq` with the same partition as the
 *          original.
 * ============================================================================
 *
 *  TODO(TICKET-ADV134 + ADV135):
 *    @Bean
 *    public DefaultErrorHandler errorHandler(KafkaTemplate<Object,Object> template) {
 *        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
 *            template,
 *            (ConsumerRecord<?,?> rec, Exception ex) ->
 *                new TopicPartition(rec.topic() + "-dlq", rec.partition()));
 *        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
 *        backoff.setMaxAttempts(3);
 *        return new DefaultErrorHandler(recoverer, backoff);
 *    }
 *
 *  GOTCHA: trade-events-dlq must already exist (TICKET-ADV128). The
 *          recoverer does NOT auto-create the topic.
 * ============================================================================
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template, KafkaErrorHandlerConfig::resolveDlqDestination);
        // 1s, 2s, 4s — three attempts total, then DLQ (TICKET-ADV135).
        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxElapsedTime(8_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        // Poison-pill messages (bad JSON etc.) go straight to DLQ — retrying
        // a deserialization failure three times is pure wasted time.
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }

    /**
     * Routes a failed record to {@code <original-topic>-dlq}, preserving the
     * original partition number so ops can correlate DLQ entries back to the
     * partition they came from. Extracted as a static method (rather than an
     * inline lambda) so the mapping is directly unit-testable.
     */
    static TopicPartition resolveDlqDestination(ConsumerRecord<?, ?> record, Exception ex) {
        return new TopicPartition(record.topic() + "-dlq", record.partition());
    }
}
