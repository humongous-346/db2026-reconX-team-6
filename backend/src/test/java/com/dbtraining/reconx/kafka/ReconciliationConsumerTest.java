package com.dbtraining.reconx.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dbtraining.reconx.dto.TradeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV131 — ReconciliationConsumer logs a recon-trigger per TradeEvent. */
class ReconciliationConsumerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(ReconciliationConsumer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void onTradeEvent_logsTradeRefAndEventType() {
        ReconciliationConsumer consumer = new ReconciliationConsumer();
        UUID eventId = UUID.randomUUID();
        TradeEvent event = new TradeEvent(eventId, "TRD-131-A", TradeEvent.EventType.TRADE_CREATED,
                Instant.now(), "trader@db.com", null, "{}");

        consumer.onTradeEvent(event);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel()).isEqualTo(Level.INFO);
        assertThat(logged.getFormattedMessage())
                .contains("TRD-131-A")
                .contains("TRADE_CREATED")
                .contains(eventId.toString());
    }

    @Test
    void onTradeEvent_logsOncePerEvent() {
        ReconciliationConsumer consumer = new ReconciliationConsumer();

        consumer.onTradeEvent(new TradeEvent(UUID.randomUUID(), "TRD-131-B",
                TradeEvent.EventType.TRADE_UPDATED, Instant.now(), "trader@db.com", "{}", "{}"));
        consumer.onTradeEvent(new TradeEvent(UUID.randomUUID(), "TRD-131-C",
                TradeEvent.EventType.TRADE_CANCELLED, Instant.now(), "admin@db.com", "{}", null));

        assertThat(appender.list).hasSize(2);
    }
}
