package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.sse.TradeSseBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TICKET-ADV104 — GET /v1/trades/stream
 *
 * Serves a persistent text/event-stream connection. Each newly created
 * trade is fanned out to every open subscription by TradeSseBroadcaster.
 * The endpoint is public so EventSource in the browser can connect without
 * a JWT (browsers cannot set custom headers on EventSource).
 */
@RestController
@RequestMapping("/v1/trades")
public class TradeStreamController {

    private final TradeSseBroadcaster broadcaster;

    public TradeStreamController(TradeSseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
