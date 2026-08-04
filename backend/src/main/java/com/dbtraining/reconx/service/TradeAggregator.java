package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TICKET-ADV137 — TradeAggregator (event-sourcing rebuild)
 *
 * WHAT:    Rebuilds a trade's current state by folding every audit_log event
 *          for that tradeRef, oldest first.
 * HOW:     TRADE_CREATED / TRADE_UPDATED set the running state to the
 *          event's after-snapshot; TRADE_CANCELLED clears it to null.
 * WHY:     Proves the event log persisted by AuditEventConsumer (ADV132) is
 *          the actual source of truth — if the `trades` table were ever
 *          dropped or corrupted, replaying from offset 0 reconstructs it.
 * OBSERVE: rebuild("TRD-001") after CREATED -> UPDATED -> CANCELLED returns
 *          Optional.empty(); without the CANCELLED it returns the last
 *          UPDATED event's after-snapshot.
 * ============================================================================
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;

    public TradeAggregator(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public Optional<String> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        String state = null;
        for (AuditLogEntry entry : events) {
            TradeEvent.EventType type = TradeEvent.EventType.valueOf(entry.getEventType());
            state = switch (type) {
                case TRADE_CREATED, TRADE_UPDATED -> entry.getAfterState();
                case TRADE_CANCELLED -> null;
            };
        }
        return Optional.ofNullable(state);
    }
}
