package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.dto.TradeEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV136 — DlqMessage flattens a TradeEvent and can reconstruct it for replay. */
class DlqMessageTest {

    @Test
    void constructor_flattensAllEventFields() {
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        TradeEvent event = new TradeEvent(eventId, "TRD-136-A", TradeEvent.EventType.TRADE_CREATED,
                timestamp, "trader@db.com", null, "{\"status\":\"PENDING\"}");
        Instant firstSeen = Instant.now();

        DlqMessage msg = new DlqMessage(event, "trade-events", 2, 42L, "boom", firstSeen);

        assertThat(msg.getEventId()).isEqualTo(eventId.toString());
        assertThat(msg.getTradeRef()).isEqualTo("TRD-136-A");
        assertThat(msg.getEventType()).isEqualTo("TRADE_CREATED");
        assertThat(msg.getEventTimestamp()).isEqualTo(timestamp);
        assertThat(msg.getActor()).isEqualTo("trader@db.com");
        assertThat(msg.getBeforeState()).isNull();
        assertThat(msg.getAfterState()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(msg.getOriginalTopic()).isEqualTo("trade-events");
        assertThat(msg.getPartition()).isEqualTo(2);
        assertThat(msg.getOffset()).isEqualTo(42L);
        assertThat(msg.getReason()).isEqualTo("boom");
        assertThat(msg.getFirstSeen()).isEqualTo(firstSeen);
    }

    @Test
    void toTradeEvent_reconstructsAnEquivalentEvent() {
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        TradeEvent original = new TradeEvent(eventId, "TRD-136-B", TradeEvent.EventType.TRADE_UPDATED,
                timestamp, "trader@db.com", "{\"n\":1}", "{\"n\":2}");

        DlqMessage msg = new DlqMessage(original, "trade-events", 0, 1L, "boom", Instant.now());
        TradeEvent rebuilt = msg.toTradeEvent();

        assertThat(rebuilt.eventId()).isEqualTo(eventId);
        assertThat(rebuilt.tradeRef()).isEqualTo("TRD-136-B");
        assertThat(rebuilt.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(rebuilt.timestamp()).isEqualTo(timestamp);
        assertThat(rebuilt.actor()).isEqualTo("trader@db.com");
        assertThat(rebuilt.before()).isEqualTo("{\"n\":1}");
        assertThat(rebuilt.after()).isEqualTo("{\"n\":2}");
    }
}
