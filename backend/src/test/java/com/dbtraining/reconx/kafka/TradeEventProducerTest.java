package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** TICKET-ADV129 — TradeEventProducer publishes keyed by tradeRef, never blocks on .get(). */
@ExtendWith(MockitoExtension.class)
class TradeEventProducerTest {

    @Mock private KafkaTemplate<String, TradeEvent> template;

    private TradeEvent sampleEvent() {
        return new TradeEvent(UUID.randomUUID(), "TRD-129-A", TradeEvent.EventType.TRADE_CREATED,
                Instant.now(), "trader@db.com", null, "{}");
    }

    @Test
    void publish_sendsToTradeEventsTopicKeyedByTradeRef() {
        when(template.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        TradeEventProducer producer = new TradeEventProducer(template);
        TradeEvent event = sampleEvent();

        producer.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(template).send(topicCaptor.capture(), keyCaptor.capture(), eq(event));
        assertThat(topicCaptor.getValue()).isEqualTo("trade-events");
        assertThat(keyCaptor.getValue()).isEqualTo(event.tradeRef());
    }

    @Test
    void publish_doesNotThrowWhenSendFails() {
        when(template.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        TradeEventProducer producer = new TradeEventProducer(template);

        // publish() must be fire-and-forget: a failed send must not propagate
        // out of publish() itself (no blocking .get(), no rethrow).
        assertThatCode(() -> producer.publish(sampleEvent())).doesNotThrowAnyException();
    }

    @Test
    void publish_logsSuccessWithoutThrowing() {
        SendResult<String, TradeEvent> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(template.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(result));
        TradeEventProducer producer = new TradeEventProducer(template);

        assertThatCode(() -> producer.publish(sampleEvent())).doesNotThrowAnyException();
    }
}
