package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * ============================================================================
 * TICKET-ADV136 — DlqConsumer
 *
 * WHAT:    Persists every message that lands on trade-events-dlq (after
 *          TICKET-ADV134's recoverer gives up) so an operator can look it up
 *          by eventId and replay it via DlqAdminController.
 * HOW:     @KafkaListener on trade-events-dlq, groupId dlq-monitor — a
 *          distinct group from recon-service/audit-service/alert-service so
 *          this doesn't compete with (or get skipped by) the main consumers.
 * WHY:     Without a persisted record, a DLQ'd message only exists on the
 *          Kafka topic itself; the replay endpoint needs a queryable store.
 * OBSERVE: Force a listener failure — a row appears in dlq_messages with the
 *          same eventId as the original TradeEvent.
 * ============================================================================
 */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;

    public DlqConsumer(DlqMessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-monitor")
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        TradeEvent event = record.value();
        log.error("DLQ: trade={} eventId={} reason={}",
                event.tradeRef(), event.eventId(), exceptionMessage);

        repo.save(new DlqMessage(event, record.topic().replace("-dlq", ""),
                record.partition(), record.offset(), exceptionMessage, Instant.now()));
    }
}
