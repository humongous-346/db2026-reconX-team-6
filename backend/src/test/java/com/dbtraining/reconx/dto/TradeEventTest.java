package com.dbtraining.reconx.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV130 — TradeEvent static factories. */
class TradeEventTest {

    @Test
    void created_hasNoBeforeState() {
        TradeEvent event = TradeEvent.created("TRD-1", "trader@db.com", "{\"status\":\"PENDING\"}");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.tradeRef()).isEqualTo("TRD-1");
        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CREATED);
        assertThat(event.actor()).isEqualTo("trader@db.com");
        assertThat(event.before()).isNull();
        assertThat(event.after()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void updated_carriesBothSnapshots() {
        TradeEvent event = TradeEvent.updated("TRD-2", "trader@db.com", "{\"status\":\"PENDING\"}", "{\"status\":\"MATCHED\"}");

        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(event.before()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(event.after()).isEqualTo("{\"status\":\"MATCHED\"}");
    }

    @Test
    void cancelled_hasNoAfterState() {
        TradeEvent event = TradeEvent.cancelled("TRD-3", "admin@db.com", "{\"status\":\"MATCHED\"}");

        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CANCELLED);
        assertThat(event.before()).isEqualTo("{\"status\":\"MATCHED\"}");
        assertThat(event.after()).isNull();
    }

    @Test
    void eachFactoryCall_generatesAFreshEventId() {
        TradeEvent a = TradeEvent.created("TRD-4", "x", "{}");
        TradeEvent b = TradeEvent.created("TRD-4", "x", "{}");

        assertThat(a.eventId()).isNotEqualTo(b.eventId());
    }
}
