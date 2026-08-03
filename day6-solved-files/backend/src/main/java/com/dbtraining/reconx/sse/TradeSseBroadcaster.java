package com.dbtraining.reconx.sse;

import com.dbtraining.reconx.dto.TradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TICKET-ADV104 (backend side) — in-process SSE broadcaster.
 *
 * Holds all active SseEmitters and fans out each new trade to every
 * subscriber. Dead emitters (client gone / timeout / error) are removed
 * lazily on the next broadcast so iteration is always on a stable snapshot.
 */
@Component
public class TradeSseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(TradeSseBroadcaster.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no server-side timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("SSE subscriber added, active={}", emitters.size());
        return emitter;
    }

    public void broadcast(TradeResponse trade) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(trade, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
