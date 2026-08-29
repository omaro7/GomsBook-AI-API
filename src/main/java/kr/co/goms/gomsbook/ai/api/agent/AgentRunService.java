package kr.co.goms.gomsbook.ai.api.agent;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentRunService {

    private final Map<String, String> pendingRuns = new ConcurrentHashMap<>();

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public String run(String message) {

        String runId = UUID.randomUUID().toString();

        pendingRuns.put(runId, message);

        return runId;

    }

    public SseEmitter subscribe(String runId) {

        String message = pendingRuns.remove(runId);

        if (message == null) {

            throw new IllegalArgumentException(
                    "Agent run was not found: "
                            + runId);

        }

        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(runId, emitter);

        emitter.onCompletion(() -> cleanup(runId));

        emitter.onTimeout(() -> cleanup(runId));

        emitter.onError(error -> cleanup(runId));

        executor.submit(
                () -> executeMockAgent(
                        runId,
                        message));

        return emitter;

    }

    private void executeMockAgent(
            String runId,
            String message) {

        try {

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.AGENT_STARTED)
                            .message("Agent 실행을 시작합니다.")
                            .build());

            sleep(500);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.ASSISTANT_MESSAGE)
                            .message("요청을 확인했습니다: " + message)
                            .build());

            sleep(500);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.RAG_STARTED)
                            .message("RAG 검색을 시작합니다.")
                            .build());

            sleep(700);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.RAG_CONTEXT)
                            .message("Mock RAG Context를 찾았습니다.")
                            .build());

            sleep(300);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.RAG_COMPLETED)
                            .message("RAG 검색이 완료되었습니다.")
                            .build());

            sleep(500);

            String toolCallId = UUID.randomUUID().toString();

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.TOOL_STARTED)
                            .message("EPUB 검사 도구를 실행합니다.")
                            .toolCallId(toolCallId)
                            .toolName("validate_epub")
                            .build());

            sleep(1000);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.TOOL_COMPLETED)
                            .message("EPUB 검사 도구 실행이 완료되었습니다.")
                            .toolCallId(toolCallId)
                            .toolName("validate_epub")
                            .build());

            sleep(500);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.ASSISTANT_MESSAGE)
                            .message("Mock Agent 테스트가 완료되었습니다.")
                            .build());

            sleep(300);

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.AGENT_COMPLETED)
                            .message("Agent 실행이 완료되었습니다.")
                            .build());

            complete(runId);

        } catch (Exception exception) {

            sendSafely(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.AGENT_FAILED)
                            .message(exception.getMessage())
                            .build());

            complete(runId);

        }

    }

    private void send(AgentEvent event)
            throws IOException {

        SseEmitter emitter =
                emitters.get(event.getRunId());

        if (emitter == null) {

            throw new IllegalStateException(
                    "SSE emitter was not found: "
                            + event.getRunId());

        }

        emitter.send(
                SseEmitter.event()
                        .name("message")
                        .data(event));

    }

    private void sendSafely(AgentEvent event) {

        try {

            send(event);

        } catch (Exception ignored) {

        }

    }

    private void complete(String runId) {

        SseEmitter emitter = emitters.remove(runId);

        if (emitter != null) {

            emitter.complete();

        }

        pendingRuns.remove(runId);

    }

    private void cleanup(String runId) {

        emitters.remove(runId);

        pendingRuns.remove(runId);

    }

    private void sleep(long milliseconds) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Agent execution was interrupted.",
                    exception);

        }

    }

}