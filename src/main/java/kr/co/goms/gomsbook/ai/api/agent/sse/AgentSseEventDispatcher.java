package kr.co.goms.gomsbook.ai.api.agent.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.co.goms.gomsbook.ai.api.agent.AgentEvent;

@Component
public class AgentSseEventDispatcher {

    private static final long DEFAULT_TIMEOUT =
            30L * 60L * 1000L;

    private final Map<String, SseEmitter> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(
            String runId) {

        if (runId == null
                || runId.isBlank()) {

            throw new IllegalArgumentException(
                    "runId must not be blank."
            );
        }

        SseEmitter emitter =
                new SseEmitter(
                        DEFAULT_TIMEOUT
                );

        emitters.put(
                runId,
                emitter
        );

        emitter.onCompletion(
                () -> remove(
                        runId,
                        emitter
                )
        );

        emitter.onTimeout(
                () -> remove(
                        runId,
                        emitter
                )
        );

        emitter.onError(
                throwable -> remove(
                        runId,
                        emitter
                )
        );

        try {

            emitter.send(
                    SseEmitter.event()
                            .comment(
                                    "connected"
                            )
            );

        } catch (IOException exception) {

            emitters.remove(
                    runId
            );

            throw new IllegalStateException(
                    "Failed to initialize SSE connection."
                            + " runId="
                            + runId,
                    exception
            );
        }

        System.out.println(
                "[GomsBook AI API] SSE subscribed"
                        + " | runId="
                        + runId
        );

        return emitter;
    }

    public void send(
            AgentEvent event) {

        if (event == null) {

            return;
        }

        String runId =
                event.getRunId();

        if (runId == null
                || runId.isBlank()) {

            System.out.println(
                    "[GomsBook AI API] SSE Event skipped"
                            + " | reason=runId missing"
                            + " | type="
                            + event.getType()
            );

            return;
        }

        SseEmitter emitter =
                emitters.get(
                        runId
                );

        if (emitter == null) {

            System.out.println(
                    "[GomsBook AI API] SSE Event skipped"
                            + " | reason=emitter not found"
                            + " | runId="
                            + runId
                            + " | type="
                            + event.getType()
            );

            return;
        }

        try {

            emitter.send(
                    SseEmitter.event()
                            .name(
                                    event
                                            .getType()
                                            .name()
                            )
                            .data(
                                    event
                            )
            );

        } catch (IOException exception) {

            emitters.remove(
                    runId
            );

            throw new IllegalStateException(
                    "Failed to send SSE event."
                            + " runId="
                            + runId
                            + ", type="
                            + event.getType(),
                    exception
            );
        }
    }

    public void complete(
            String runId) {

        SseEmitter emitter =
                emitters.remove(
                        runId
                );

        if (emitter == null) {

            return;
        }

        emitter.complete();
    }

    private void remove(
            String runId,
            SseEmitter emitter) {

        emitters.remove(
                runId,
                emitter
        );
    }
}