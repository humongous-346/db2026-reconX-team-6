package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** TICKET-ADV137 — TradeAggregator folds audit_log events into current state. */
@ExtendWith(MockitoExtension.class)
class TradeAggregatorTest {

    @Mock private AuditLogRepository auditRepo;

    private AuditLogEntry entry(String eventType, String after) {
        return new AuditLogEntry("evt-" + Math.random(), "TRD-137-A", eventType,
                Instant.now(), "trader@db.com", null, after);
    }

    @Test
    void rebuild_noEvents_returnsEmpty() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-137-Z")).thenReturn(List.of());
        TradeAggregator aggregator = new TradeAggregator(auditRepo);

        assertThat(aggregator.rebuild("TRD-137-Z")).isEmpty();
    }

    @Test
    void rebuild_createdThenUpdated_returnsLatestAfterSnapshot() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-137-A")).thenReturn(List.of(
                entry("TRADE_CREATED", "{\"status\":\"PENDING\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"MATCHED\"}")
        ));
        TradeAggregator aggregator = new TradeAggregator(auditRepo);

        Optional<String> state = aggregator.rebuild("TRD-137-A");

        assertThat(state).contains("{\"status\":\"MATCHED\"}");
    }

    @Test
    void rebuild_createdUpdatedThenCancelled_returnsEmpty() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-137-A")).thenReturn(List.of(
                entry("TRADE_CREATED", "{\"status\":\"PENDING\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"MATCHED\"}"),
                entry("TRADE_CANCELLED", null)
        ));
        TradeAggregator aggregator = new TradeAggregator(auditRepo);

        assertThat(aggregator.rebuild("TRD-137-A")).isEmpty();
    }
}
