package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** TICKET-ADV132 — AuditEventConsumer persists every TradeEvent to audit_log. */
@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock private AuditLogRepository repo;

    @Test
    void onTradeEvent_persistsAllFieldsFromTheEvent() {
        AuditEventConsumer consumer = new AuditEventConsumer(repo);
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        TradeEvent event = new TradeEvent(eventId, "TRD-132-A", TradeEvent.EventType.TRADE_CREATED,
                timestamp, "trader@db.com", null, "{\"status\":\"PENDING\"}");

        consumer.onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repo).save(captor.capture());
        AuditLogEntry saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(eventId.toString());
        assertThat(saved.getTradeRef()).isEqualTo("TRD-132-A");
        assertThat(saved.getEventType()).isEqualTo("TRADE_CREATED");
        assertThat(saved.getEventTimestamp()).isEqualTo(timestamp);
        assertThat(saved.getActor()).isEqualTo("trader@db.com");
        assertThat(saved.getBeforeState()).isNull();
        assertThat(saved.getAfterState()).isEqualTo("{\"status\":\"PENDING\"}");
    }

    @Test
    void onTradeEvent_savesOneRowPerEvent() {
        AuditEventConsumer consumer = new AuditEventConsumer(repo);

        for (int i = 0; i < 10; i++) {
            TradeEvent event = new TradeEvent(UUID.randomUUID(), "TRD-132-B", TradeEvent.EventType.TRADE_UPDATED,
                    Instant.now(), "trader@db.com", "{\"n\":" + i + "}", "{\"n\":" + (i + 1) + "}");
            consumer.onTradeEvent(event);
        }

        verify(repo, org.mockito.Mockito.times(10)).save(org.mockito.ArgumentMatchers.any());
    }
}
