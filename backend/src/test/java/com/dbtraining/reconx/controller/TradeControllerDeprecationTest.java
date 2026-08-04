package com.dbtraining.reconx.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** TICKET-ADV080 — deprecated /v1/trades/old-search returns 410 Gone + deprecation headers. */
@ExtendWith(MockitoExtension.class)
class TradeControllerDeprecationTest {

    @Mock private HttpServletResponse response;

    @Test
    void oldSearch_returns410WithDeprecationHeaders() {
        TradeController controller = new TradeController(null, null);

        ResponseEntity<Void> result = controller.oldSearch(response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.GONE);
        verify(response).setHeader("Deprecation", "true");
        verify(response).setHeader("Sunset", "Sat, 1 Jul 2026 00:00:00 GMT");
        verify(response).setHeader("Link", "</api/v1/trades>; rel=\"successor-version\"");
    }
}
