package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ResolutionRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** TICKET-ADV070 — PUT /api/v1/recon/results/{id}/resolve. */
@ExtendWith(MockitoExtension.class)
class ReconControllerResolveTest {

    @Mock private ReconBreakRepository breaks;

    @Test
    void resolve_setsResolvedStatusAndNote() {
        ReconController controller = new ReconController(breaks);
        ReconBreak rb = new ReconBreak();
        when(breaks.findById(1L)).thenReturn(Optional.of(rb));
        when(breaks.save(rb)).thenReturn(rb);

        ResponseEntity<ReconBreak> response = controller.resolve(1L, new ResolutionRequest("Confirmed via counterparty email."));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getBody().getResolvedAt()).isNotNull();
        assertThat(response.getBody().getResolutionNote()).isEqualTo("Confirmed via counterparty email.");
    }

    @Test
    void resolve_throwsWhenBreakIsMissing() {
        ReconController controller = new ReconController(breaks);
        when(breaks.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.resolve(404L, new ResolutionRequest("note")))
                .isInstanceOf(TradeNotFoundException.class);
    }
}
