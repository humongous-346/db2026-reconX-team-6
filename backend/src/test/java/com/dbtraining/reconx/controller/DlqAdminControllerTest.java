package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** TICKET-ADV136 — DlqAdminController: dry-run preview vs real one-at-a-time replay. */
@ExtendWith(MockitoExtension.class)
class DlqAdminControllerTest {

    @Mock private DlqMessageRepository repo;
    @Mock private TradeEventProducer producer;

    private DlqMessage sampleMessage(UUID eventId) {
        TradeEvent event = new TradeEvent(eventId, "TRD-136-D", TradeEvent.EventType.TRADE_CREATED,
                Instant.now(), "trader@db.com", null, "{}");
        return new DlqMessage(event, "trade-events", 0, 5L, "boom", Instant.now());
    }

    @Test
    void replay_dryRun_previewsWithoutPublishingOrDeleting() {
        DlqAdminController controller = new DlqAdminController(repo, producer);
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.of(sampleMessage(eventId)));

        ResponseEntity<Map<String, Object>> response = controller.replay(eventId, true);

        assertThat(response.getBody().get("dryRun")).isEqualTo(true);
        assertThat(response.getBody().get("wouldReplayTo")).isEqualTo("trade-events");
        verify(producer, never()).publish(any());
        verify(repo, never()).delete(any());
    }

    @Test
    void replay_realRun_publishesAndDeletesTheDlqRow() {
        DlqAdminController controller = new DlqAdminController(repo, producer);
        UUID eventId = UUID.randomUUID();
        DlqMessage msg = sampleMessage(eventId);
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.of(msg));

        ResponseEntity<Map<String, Object>> response = controller.replay(eventId, false);

        assertThat(response.getBody().get("replayed")).isEqualTo(true);
        verify(producer).publish(argThat(e -> e.eventId().equals(eventId)));
        verify(repo).delete(msg);
    }

    @Test
    void replay_unknownEventId_throws() {
        DlqAdminController controller = new DlqAdminController(repo, producer);
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.replay(eventId, false))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(producer);
    }
}
