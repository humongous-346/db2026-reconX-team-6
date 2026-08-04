package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** TICKET-ADV136 — DlqConsumer persists a DlqMessage per record on trade-events-dlq. */
@ExtendWith(MockitoExtension.class)
class DlqConsumerTest {

    @Mock private DlqMessageRepository repo;

    @Test
    void onDlqMessage_persistsWithOriginalTopicPartitionAndReason() {
        DlqConsumer consumer = new DlqConsumer(repo);
        UUID eventId = UUID.randomUUID();
        TradeEvent event = new TradeEvent(eventId, "TRD-136-C", TradeEvent.EventType.TRADE_CREATED,
                Instant.now(), "trader@db.com", null, "{}");
        ConsumerRecord<String, TradeEvent> record =
                new ConsumerRecord<>("trade-events-dlq", 2, 17L, "TRD-136-C", event);

        consumer.onDlqMessage(record, "boom: deserialization failed");

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(repo).save(captor.capture());
        DlqMessage saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(eventId.toString());
        assertThat(saved.getTradeRef()).isEqualTo("TRD-136-C");
        // "-dlq" suffix is stripped so replay targets the ORIGINAL topic, not the DLQ itself.
        assertThat(saved.getOriginalTopic()).isEqualTo("trade-events");
        assertThat(saved.getPartition()).isEqualTo(2);
        assertThat(saved.getOffset()).isEqualTo(17L);
        assertThat(saved.getReason()).isEqualTo("boom: deserialization failed");
    }
}
