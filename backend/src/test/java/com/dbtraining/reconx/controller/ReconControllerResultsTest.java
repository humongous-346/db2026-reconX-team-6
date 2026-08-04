package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** TICKET-ADV069 — GET /api/v1/recon/jobs/{jobId}/results. */
@ExtendWith(MockitoExtension.class)
class ReconControllerResultsTest {

    @Mock private ReconBreakRepository breaks;

    @Test
    void results_returnsOpenBreaksFromRepository() {
        ReconController controller = new ReconController(breaks);
        ReconBreak breakRow = new ReconBreak();
        when(breaks.findAll()).thenReturn(List.of(breakRow));

        List<ReconBreak> results = controller.results("any-job-id");

        assertThat(results).containsExactly(breakRow);
    }
}
