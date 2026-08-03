package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}. */
@ExtendWith(MockitoExtension.class)
class AuditControllerHistoryTest {

    @Mock private AuditLogRepository auditRepo;

    @Test
    void history_returnsRevisionsOldestFirstFromRepository() {
        AuditController controller = new AuditController(auditRepo);
        AuditLogEntry entry = new AuditLogEntry();
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-20260315-0001"))
                .thenReturn(List.of(entry));

        List<AuditLogEntry> result = controller.history("TRD-20260315-0001");

        assertThat(result).containsExactly(entry);
    }
}
