package com.dbtraining.reconx.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV133 — AlertConsumer logs every system-alerts message at WARN. */
class AlertConsumerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(AlertConsumer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void onAlert_logsPayloadAtWarnLevel() {
        AlertConsumer consumer = new AlertConsumer();

        consumer.onAlert("recon-break-threshold-exceeded");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("recon-break-threshold-exceeded");
    }

    @Test
    void onAlert_logsEachDistinctPayload() {
        AlertConsumer consumer = new AlertConsumer();

        consumer.onAlert("alert-one");
        consumer.onAlert("alert-two");

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("alert-one");
        assertThat(appender.list.get(1).getFormattedMessage()).contains("alert-two");
    }
}
