package kr.co.goms.gomsbook.ai.api.agent;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.co.goms.gomsbook.ai.api.agent.bridge.AgentEngineBridge;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

@Service
public class AgentRunService {

    private final AgentEngineBridge agentEngineBridge;

    private final Map<String, String> pendingRuns = new ConcurrentHashMap<>();

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentRunService(AgentEngineBridge agentEngineBridge) {

        this.agentEngineBridge = agentEngineBridge;
    }

    public String run(String message) {

        String runId = UUID.randomUUID().toString();

        pendingRuns.put(runId, message);

        return runId;
    }

    public SseEmitter subscribe(String runId) {

        String message = pendingRuns.remove(runId);

        if (message == null) throw new IllegalArgumentException("Agent run was not found: " + runId);

        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(runId, emitter);

        emitter.onCompletion(() -> cleanup(runId));

        emitter.onTimeout(() -> cleanup(runId));

        emitter.onError(error -> cleanup(runId));

        executor.submit(() -> executeAgent(runId, message));

        return emitter;
    }

    public void approve(String runId, String approvalId) {

        PendingApproval approval = findPendingApproval(runId, approvalId);

        pendingApprovals.remove(approvalId);

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_APPROVED)
                        .message("사용자가 작업을 승인했습니다.")
                        .approvalId(approvalId)
                        .build()
        );

        executor.submit(() -> executeApprovedAction(approval));
    }

    public void reject(String runId, String approvalId) {

        PendingApproval approval = findPendingApproval(runId, approvalId);

        pendingApprovals.remove(approvalId);

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_REJECTED)
                        .message("사용자가 작업을 취소했습니다.")
                        .approvalId(approvalId)
                        .build()
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.ASSISTANT_MESSAGE)
                        .message(approval.fileName + " 생성을 취소했습니다.")
                        .build()
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.AGENT_COMPLETED)
                        .build()
        );

        complete(runId);
    }

    private void executeAgent(String runId, String message) {

        try {

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.AGENT_STARTED)
                            .message("Agent 실행을 시작합니다.")
                            .build()
            );

            if (isAuthorGenerationRequest(message)) {

                requestAuthorApproval(runId);

                return;
            }

            String response = agentEngineBridge.generate(
                    runId,
                    message,
                    toolResult -> handleToolResult(
                            runId,
                            toolResult
                    )
            );

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.ASSISTANT_MESSAGE)
                            .message(response)
                            .build()
            );

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(AgentEventType.AGENT_COMPLETED)
                            .build()
            );

            complete(runId);

        } catch (Exception exception) {

            fail(runId, exception);
        }
    }

    private void handleToolResult(String runId, ToolResult toolResult) {

        if (toolResult == null) return;

        AgentEvent.AgentEventBuilder builder = AgentEvent.builder()
                .runId(runId)
                .toolCallId(toolResult.getToolCallId())
                .toolName(toolResult.getToolName())
                .message(resolveToolMessage(toolResult))
                .data(toolResult.hasData() ? toolResult.getData() : null);

        if (toolResult.hasError()) {

            builder.type(
                    AgentEventType.TOOL_FAILED
            );

        } else {

            builder.type(
                    AgentEventType.TOOL_COMPLETED
            );
        }

        sendSafely(
                builder.build()
        );
    }

    private String resolveToolMessage(ToolResult toolResult) {

        if (toolResult.hasMessage()) return toolResult.getMessage();

        if (toolResult.hasError()) {

            String errorMessage = toolResult.getErrorMessage();

            if (errorMessage != null && !errorMessage.isBlank()) return errorMessage;

            return "도구 실행에 실패했습니다.";
        }

        return "도구 실행이 정상적으로 완료되었습니다.";
    }

    private void requestAuthorApproval(String runId) throws IOException {

        String approvalId = UUID.randomUUID().toString();

        String fileName = "author.xhtml";

        String content = createMockAuthorXhtml();

        PendingApproval approval = new PendingApproval(
                approvalId,
                runId,
                "CREATE_EPUB_AUTHOR",
                fileName,
                content
        );

        pendingApprovals.put(approvalId, approval);

        send(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_REQUIRED)
                        .message("다음 내용으로 author.xhtml을 생성하시겠습니까?")
                        .approvalId(approvalId)
                        .title("author.xhtml 생성")
                        .fileName(fileName)
                        .content(content)
                        .build()
        );
    }

    private void executeApprovedAction(PendingApproval approval) {

        try {

            if ("CREATE_EPUB_AUTHOR".equals(approval.action)) executeCreateAuthor(approval);

            send(
                    AgentEvent.builder()
                            .runId(approval.runId)
                            .type(AgentEventType.AGENT_COMPLETED)
                            .build()
            );

            complete(approval.runId);

        } catch (Exception exception) {

            fail(approval.runId, exception);
        }
    }

    private void executeCreateAuthor(PendingApproval approval) throws IOException {

        send(
                AgentEvent.builder()
                        .runId(approval.runId)
                        .type(AgentEventType.TOOL_STARTED)
                        .message(approval.fileName + " 생성을 시작합니다.")
                        .toolName("generate_epub_author")
                        .build()
        );

        /*
         * 현재 승인 실행은 Mock 단계입니다.
         * Core EPUB Tool 이관 후 AgentEngineBridge.executeApproved()로 연결합니다.
         */

        sleep(500);

        send(
                AgentEvent.builder()
                        .runId(approval.runId)
                        .type(AgentEventType.TOOL_COMPLETED)
                        .message(approval.fileName + " 생성이 완료되었습니다.")
                        .toolName("generate_epub_author")
                        .build()
        );

        send(
                AgentEvent.builder()
                        .runId(approval.runId)
                        .type(AgentEventType.ASSISTANT_MESSAGE)
                        .message(approval.fileName + "을 생성했습니다.")
                        .build()
        );
    }

    private PendingApproval findPendingApproval(String runId, String approvalId) {

        if (approvalId == null || approvalId.isBlank()) throw new IllegalArgumentException("approvalId must not be blank");

        PendingApproval approval = pendingApprovals.get(approvalId);

        if (approval == null) throw new IllegalArgumentException("Pending approval was not found: " + approvalId);

        if (!approval.runId.equals(runId)) throw new IllegalArgumentException("Approval run ID does not match: " + runId);

        return approval;
    }

    private boolean isAuthorGenerationRequest(String message) {

        if (message == null) return false;

        String normalized = message.toLowerCase();

        return normalized.contains("author.xhtml") || normalized.contains("작가소개") || normalized.contains("작가 소개");
    }

    private String createMockAuthorXhtml() {

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:epub="http://www.idpf.org/2007/ops"
                      lang="ko"
                      xml:lang="ko">
                <head>
                    <meta charset="UTF-8"/>
                    <title>작가 소개</title>
                </head>
                <body>
                    <section epub:type="bio" aria-labelledby="author-title">
                        <h1 id="author-title">작가 소개</h1>
                        <p>작가 소개 내용입니다.</p>
                    </section>
                </body>
                </html>
                """;
    }

    private void send(AgentEvent event) throws IOException {

        SseEmitter emitter = emitters.get(event.getRunId());

        if (emitter == null) throw new IllegalStateException("SSE emitter was not found: " + event.getRunId());

        emitter.send(
                SseEmitter.event()
                        .name("message")
                        .data(event)
        );
    }

    private void sendSafely(AgentEvent event) {

        try {

            send(event);

        } catch (Exception exception) {

            System.err.println(
                    "[GomsBook AI API] Failed to send SSE event: "
                            + exception.getMessage()
            );
        }
    }

    private void fail(String runId, Exception exception) {

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.AGENT_FAILED)
                        .message(resolveErrorMessage(exception))
                        .build()
        );

        complete(runId);
    }

    private String resolveErrorMessage(Exception exception) {

        if (exception == null) return "Agent 실행 중 오류가 발생했습니다.";

        String message = exception.getMessage();

        return message == null || message.isBlank() ? "Agent 실행 중 오류가 발생했습니다." : message;
    }

    private void complete(String runId) {

        SseEmitter emitter = emitters.remove(runId);

        if (emitter != null) emitter.complete();

        pendingRuns.remove(runId);

        pendingApprovals.entrySet().removeIf(
                entry -> entry.getValue().runId.equals(runId)
        );
    }

    private void cleanup(String runId) {

        emitters.remove(runId);

        pendingRuns.remove(runId);

        pendingApprovals.entrySet().removeIf(
                entry -> entry.getValue().runId.equals(runId)
        );
    }

    private void sleep(long milliseconds) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Agent execution was interrupted.",
                    exception
            );
        }
    }

    private static class PendingApproval {

        private final String approvalId;

        private final String runId;

        private final String action;

        private final String fileName;

        private final String content;

        private PendingApproval(
                String approvalId,
                String runId,
                String action,
                String fileName,
                String content) {

            this.approvalId = approvalId;

            this.runId = runId;

            this.action = action;

            this.fileName = fileName;

            this.content = content;
        }
    }
}