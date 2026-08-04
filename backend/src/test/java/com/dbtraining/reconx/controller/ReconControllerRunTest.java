package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV068 — POST /api/v1/recon/run returns 202 + jobId + Location. */
@ExtendWith(MockitoExtension.class)
class ReconControllerRunTest {

    @Mock private ReconBreakRepository breaks;

    @Test
    void runRecon_returns202WithJobIdAndLocation() {
        ReconController controller = new ReconController(breaks);
        ReconRunRequest req = new ReconRunRequest(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        ResponseEntity<Map<String, String>> response = controller.runRecon(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String jobId = response.getBody().get("jobId");
        assertThat(UUID.fromString(jobId)).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("QUEUED");
        assertThat(response.getHeaders().getLocation().toString()).contains("/recon/jobs/" + jobId + "/results");
    }
}
