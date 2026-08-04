package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * TICKET-ADV136 — DlqAdminController
 *
 * WHAT:    Operator escape hatch for replaying a single trade-events-dlq
 *          message back onto trade-events once the underlying bug is fixed.
 * HOW:     POST /api/v1/admin/dlq/replay?eventId=...&dryRun=... Looks the
 *          message up by eventId (never bulk-replays), and on a real (non
 *          dry) run re-publishes via TradeEventProducer then deletes the row.
 * WHY:     Bulk replay would just re-DLQ the same messages if the root cause
 *          isn't fixed yet — one-at-a-time forces a deliberate operator
 *          decision per message.
 * OBSERVE: ADMIN token + dryRun=true previews the replay target without
 *          side effects; TRADER token gets 403; no token gets 401 (once
 *          TICKET-ADV074's SecurityFilterChain + @EnableMethodSecurity land).
 * ============================================================================
 */
@RestController
@RequestMapping("/v1/admin/dlq")
@Tag(name = "admin", description = "DLQ inspection and replay")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;

    public DlqAdminController(DlqMessageRepository repo, TradeEventProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    @PostMapping("/replay")
    @Operation(summary = "Replay a single DLQ'd message back onto trade-events")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun) {

        DlqMessage msg = repo.findByEventId(eventId.toString())
                .orElseThrow(() -> new IllegalArgumentException("No DLQ message: " + eventId));

        if (dryRun) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "wouldReplayTo", msg.getOriginalTopic(),
                    "tradeRef", msg.getTradeRef()
            ));
        }

        producer.publish(msg.toTradeEvent());
        repo.delete(msg);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", eventId,
                "topic", msg.getOriginalTopic()
        ));
    }
}
