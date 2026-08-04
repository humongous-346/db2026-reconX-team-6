package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** TICKET-ADV138 — GET /api/v1/audit/trades/{tradeRef}/events. */
@ExtendWith(MockitoExtension.class)
class AuditControllerEventsTest {

    @Mock private AuditLogRepository auditRepo;

    @Test
    void events_returnsRepositoryResultOldestFirst() {
        AuditController controller = new AuditController(auditRepo);
        AuditLogEntry created = new AuditLogEntry("evt-1", "TRD-138-A", "TRADE_CREATED",
                Instant.now(), "trader@db.com", null, "{\"status\":\"PENDING\"}");
        AuditLogEntry updated = new AuditLogEntry("evt-2", "TRD-138-A", "TRADE_UPDATED",
                Instant.now(), "trader@db.com", "{\"status\":\"PENDING\"}", "{\"status\":\"MATCHED\"}");
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-138-A"))
                .thenReturn(List.of(created, updated));

        List<AuditLogEntry> result = controller.events("TRD-138-A");

        assertThat(result).containsExactly(created, updated);
    }

    @Test
    void events_noHistory_returnsEmptyList() {
        AuditController controller = new AuditController(auditRepo);
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-138-UNKNOWN"))
                .thenReturn(List.of());

        assertThat(controller.events("TRD-138-UNKNOWN")).isEmpty();
    }
}
