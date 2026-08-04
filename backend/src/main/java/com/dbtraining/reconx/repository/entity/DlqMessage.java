package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.dto.TradeEvent;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-ADV136 — one row per message quarantined by the trade-events-dlq
 * DeadLetterPublishingRecoverer (TICKET-ADV134). Flattens the original
 * TradeEvent's fields (same approach as AuditLogEntry) plus DLQ-specific
 * routing metadata, so a message can be looked up by eventId and replayed.
 */
@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(length = 100)
    private String actor;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "kafka_partition", nullable = false)
    private Integer partition;

    @Column(name = "kafka_offset", nullable = false)
    private Long offset;

    @Column(length = 1000)
    private String reason;

    @Column(name = "first_seen")
    private Instant firstSeen;

    public DlqMessage() {}

    public DlqMessage(TradeEvent event, String originalTopic, int partition, long offset,
                      String reason, Instant firstSeen) {
        this.eventId = event.eventId().toString();
        this.tradeRef = event.tradeRef();
        this.eventType = event.eventType().name();
        this.eventTimestamp = event.timestamp();
        this.actor = event.actor();
        this.beforeState = event.before();
        this.afterState = event.after();
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    public Long getId()               { return id; }
    public String getEventId()        { return eventId; }
    public String getTradeRef()       { return tradeRef; }
    public String getEventType()      { return eventType; }
    public Instant getEventTimestamp(){ return eventTimestamp; }
    public String getActor()          { return actor; }
    public String getBeforeState()    { return beforeState; }
    public String getAfterState()     { return afterState; }
    public String getOriginalTopic()  { return originalTopic; }
    public Integer getPartition()     { return partition; }
    public Long getOffset()           { return offset; }
    public String getReason()         { return reason; }
    public Instant getFirstSeen()     { return firstSeen; }

    /** Reconstructs the original TradeEvent so it can be re-published on replay. */
    public TradeEvent toTradeEvent() {
        return new TradeEvent(UUID.fromString(eventId), tradeRef,
                TradeEvent.EventType.valueOf(eventType), eventTimestamp, actor, beforeState, afterState);
    }
}
