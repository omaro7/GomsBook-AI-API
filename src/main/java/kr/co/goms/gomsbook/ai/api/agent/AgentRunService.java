package kr.co.goms.gomsbook.ai.api.agent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.co.goms.gomsbook.ai.agent.approval.AgentApproval;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalService;
import kr.co.goms.gomsbook.ai.api.agent.bridge.AgentEngineBridge;
import kr.co.goms.gomsbook.ai.api.agent.sse.AgentSseEventDispatcher;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

@Service
public class AgentRunService {

    private final AgentEngineBridge agentEngineBridge;

    private final AgentSseEventDispatcher sseDispatcher;

    private final AgentApprovalService approvalService;

    private final AgentApprovalExecutor approvalExecutor;

    private final Map<String, String> pendingRuns =
            new ConcurrentHashMap<>();

    private final ExecutorService executor =
            Executors.newCachedThreadPool();

    public AgentRunService(
            AgentEngineBridge agentEngineBridge,
            AgentSseEventDispatcher sseDispatcher,
            AgentApprovalService approvalService,
            AgentApprovalExecutor approvalExecutor) {

        this.agentEngineBridge =
                agentEngineBridge;

        this.sseDispatcher =
                sseDispatcher;

        this.approvalService =
                approvalService;

        this.approvalExecutor =
                approvalExecutor;
    }

    public String run(
            String message) {

        String runId =
                UUID.randomUUID()
                        .toString();

        pendingRuns.put(
                runId,
                message
        );

        return runId;
    }

    public SseEmitter subscribe(String runId) {

        SseEmitter emitter = sseDispatcher.subscribe(runId);

        String message = pendingRuns.remove(runId);

        if (message != null) {

            executor.submit(
                    () -> executeAgent(
                            runId,
                            message
                    )
            );
        }

        return emitter;
    }

    public void approve( String runId, String approvalId) {

        AgentApproval approval = approvalService.get(approvalId);

        validateRun( runId, approval);

        approvalService.approve(approvalId);

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.APPROVAL_APPROVED
                        )
                        .message(
                                "사용자가 작업을 승인했습니다."
                        )
                        .approvalId(
                                approval.getApprovalId()
                        )
                        .title(
                                approval.getTitle()
                        )
                        .fileName(
                                approval.getFileName()
                        )
                        .build()
        );

        executor.submit(
                () -> executeApprovedAction(
                        approval
                )
        );
    }

    public void reject(
            String runId,
            String approvalId) {

        AgentApproval approval =
                approvalService.get(
                        approvalId
                );

        validateRun(
                runId,
                approval
        );

        approvalService.reject(
                approvalId
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.APPROVAL_REJECTED
                        )
                        .message(
                                "사용자가 작업을 취소했습니다."
                        )
                        .approvalId(
                                approval.getApprovalId()
                        )
                        .title(
                                approval.getTitle()
                        )
                        .fileName(
                                approval.getFileName()
                        )
                        .build()
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.ASSISTANT_MESSAGE
                        )
                        .message(
                                approval.getFileName()
                                        + " 생성을 취소했습니다."
                        )
                        .build()
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.AGENT_COMPLETED
                        )
                        .build()
        );

        complete(
                runId
        );
    }

    private void executeAgent(
            String runId,
            String message) {

        try {

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(
                                    AgentEventType.AGENT_STARTED
                            )
                            .message(
                                    "Agent 실행을 시작합니다."
                            )
                            .build()
            );

            String response =
                    agentEngineBridge.generate(
                            runId,
                            message,
                            toolResult -> handleToolResult(
                                    runId,
                                    toolResult
                            )
                    );

            if (response != null
                    && !response.isBlank()) {

                send(
                        AgentEvent.builder()
                                .runId(runId)
                                .type(
                                        AgentEventType.ASSISTANT_MESSAGE
                                )
                                .message(
                                        response
                                )
                                .build()
                );
            }

            /*
             * 승인 대기 중이면 SSE를 종료하지 않습니다.
             *
             * 이후 approve/reject 호출에서
             * AGENT_COMPLETED 및 complete가 실행됩니다.
             */
            if (hasPendingApproval(
                    runId
            )) {

                System.out.println(
                        "[GomsBook AI API] Agent waiting for approval"
                                + " | runId="
                                + runId
                );

                return;
            }

            send(
                    AgentEvent.builder()
                            .runId(runId)
                            .type(
                                    AgentEventType.AGENT_COMPLETED
                            )
                            .build()
            );

            complete(
                    runId
            );

        } catch (Exception exception) {

            fail(
                    runId,
                    exception
            );
        }
    }

    private void handleToolResult(
            String runId,
            ToolResult toolResult) {

        if (toolResult == null) {

            return;
        }

        AgentEventType type =
                toolResult.hasError()
                        ? AgentEventType.TOOL_FAILED
                        : AgentEventType.TOOL_COMPLETED;

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(type)
                        .message(
                                resolveToolMessage(
                                        toolResult
                                )
                        )
                        .toolCallId(
                                toolResult.getToolCallId()
                        )
                        .toolName(
                                toolResult.getToolName()
                        )
                        .data(
                                toolResult.hasData()
                                        ? toolResult.getData()
                                        : null
                        )
                        .build()
        );

        if (!isApprovalRequired(
                toolResult
        )) {

            return;
        }

        String approvalId =
                getDataString(
                        toolResult,
                        "approvalId"
                );

        String title =
                getDataString(
                        toolResult,
                        "title"
                );

        String message =
                getDataString(
                        toolResult,
                        "message"
                );

        String fileName =
                getDataString(
                        toolResult,
                        "fileName"
                );

        String content =
                getDataString(
                        toolResult,
                        "content"
                );

        if (approvalId == null
                || approvalId.isBlank()) {

            throw new IllegalStateException(
                    "Approval ToolResult에 approvalId가 없습니다."
            );
        }

        PendingApprovalRunHolder.add(
                runId
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.APPROVAL_REQUIRED
                        )
                        .message(message)
                        .toolName(
                                toolResult.getToolName()
                        )
                        .approvalId(
                                approvalId
                        )
                        .title(
                                title
                        )
                        .fileName(
                                fileName
                        )
                        .content(
                                content
                        )
                        .build()
        );

        System.out.println(
                "[GomsBook AI API] Approval Required"
                        + " | runId="
                        + runId
                        + " | toolName="
                        + toolResult.getToolName()
                        + " | approvalId="
                        + approvalId
                        + " | fileName="
                        + fileName
        );
    }

    private boolean isApprovalRequired(
            ToolResult toolResult) {

        if (toolResult == null
                || !toolResult.hasData()
                || toolResult.getData() == null) {

            return false;
        }

        Object value =
                toolResult
                        .getData()
                        .get(
                                "approvalRequired"
                        );

        if (value instanceof Boolean) {

            return ((Boolean) value)
                    .booleanValue();
        }

        return value != null
                && Boolean.parseBoolean(
                        String.valueOf(
                                value
                        )
                );
    }

    private boolean hasPendingApproval(
            String runId) {

        return PendingApprovalRunHolder
                .contains(
                        runId
                );
    }

    private void executeApprovedAction(
            AgentApproval approval) {

        try {

            send(
                    AgentEvent.builder()
                            .runId(
                                    approval.getRunId()
                            )
                            .type(
                                    AgentEventType.TOOL_STARTED
                            )
                            .message(
                                    approval.getFileName()
                                            + " 생성을 시작합니다."
                            )
                            .toolName(
                                    approval.getAction()
                            )
                            .approvalId(
                                    approval.getApprovalId()
                            )
                            .title(
                                    approval.getTitle()
                            )
                            .fileName(
                                    approval.getFileName()
                            )
                            .build()
            );

            approvalExecutor.execute(
                    approval
            );

            send(
                    AgentEvent.builder()
                            .runId(
                                    approval.getRunId()
                            )
                            .type(
                                    AgentEventType.TOOL_COMPLETED
                            )
                            .message(
                                    approval.getFileName()
                                            + " 생성이 완료되었습니다."
                            )
                            .toolName(
                                    approval.getAction()
                            )
                            .approvalId(
                                    approval.getApprovalId()
                            )
                            .title(
                                    approval.getTitle()
                            )
                            .fileName(
                                    approval.getFileName()
                            )
                            .build()
            );

            send(
                    AgentEvent.builder()
                            .runId(
                                    approval.getRunId()
                            )
                            .type(
                                    AgentEventType.ASSISTANT_MESSAGE
                            )
                            .message(
                                    approval.getFileName()
                                            + "을 생성했습니다."
                            )
                            .build()
            );

            send(
                    AgentEvent.builder()
                            .runId(
                                    approval.getRunId()
                            )
                            .type(
                                    AgentEventType.AGENT_COMPLETED
                            )
                            .build()
            );

            complete(
                    approval.getRunId()
            );

        } catch (Exception exception) {

        	 sendSafely(
                     AgentEvent.builder()
                             .runId(
                                     approval.getRunId()
                             )
                             .type(
                                     AgentEventType.TOOL_FAILED
                             )
                             .message(
                                     resolveErrorMessage(
                                             exception
                                     )
                             )
                             .toolName(
                                     approval.getAction()
                             )
                             .approvalId(
                                     approval.getApprovalId()
                             )
                             .title(
                                     approval.getTitle()
                             )
                             .fileName(
                                     approval.getFileName()
                             )
                             .build()
             );
        	 
            fail(
                    approval.getRunId(),
                    exception
            );
        }
    }

    private void validateRun(
            String runId,
            AgentApproval approval) {

        if (runId == null
                || runId.isBlank()) {

            throw new IllegalArgumentException(
                    "runId must not be blank."
            );
        }

        if (approval == null) {

            throw new IllegalArgumentException(
                    "approval must not be null."
            );
        }

        if (!runId.equals(
                approval.getRunId()
        )) {

            throw new IllegalArgumentException(
                    "Approval run ID does not match."
                            + " requestRunId="
                            + runId
                            + ", approvalRunId="
                            + approval.getRunId()
            );
        }

        System.out.println(
                "[GomsBook AI API] Approval Run Validation"
                        + " | status=VALID"
                        + " | runId="
                        + runId
                        + " | approvalId="
                        + approval.getApprovalId()
        );
    }

    private String resolveToolMessage(
            ToolResult toolResult) {

        if (toolResult.hasMessage()) {

            return toolResult.getMessage();
        }

        if (toolResult.hasError()) {

            String errorMessage =
                    toolResult.getErrorMessage();

            if (errorMessage != null
                    && !errorMessage.isBlank()) {

                return errorMessage;
            }

            return "도구 실행에 실패했습니다.";
        }

        return "도구 실행이 정상적으로 완료되었습니다.";
    }

    private String getDataString(
            ToolResult toolResult,
            String name) {

        if (toolResult == null
                || !toolResult.hasData()
                || toolResult.getData() == null) {

            return null;
        }

        Object value =
                toolResult
                        .getData()
                        .get(name);

        return value == null
                ? null
                : String.valueOf(
                        value
                );
    }

    private void send(
            AgentEvent event) {

        sseDispatcher.send(
                event
        );
    }

    private void sendSafely(
            AgentEvent event) {

        try {

            send(
                    event
            );

        } catch (Exception exception) {

            System.err.println(
                    "[GomsBook AI API] Failed to send SSE event: "
                            + exception.getMessage()
            );
        }
    }

    private void fail(
            String runId,
            Exception exception) {

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(
                                AgentEventType.AGENT_FAILED
                        )
                        .message(
                                resolveErrorMessage(
                                        exception
                                )
                        )
                        .build()
        );

        complete(
                runId
        );
    }

    private String resolveErrorMessage(
            Exception exception) {

        if (exception == null) {

            return "Agent 실행 중 오류가 발생했습니다.";
        }

        String message =
                exception.getMessage();

        return message == null
                || message.isBlank()
                        ? "Agent 실행 중 오류가 발생했습니다."
                        : message;
    }

    private void complete(
            String runId) {

        sseDispatcher.complete(
                runId
        );

        pendingRuns.remove(
                runId
        );

        PendingApprovalRunHolder.remove(
                runId
        );
    }

    private static final class PendingApprovalRunHolder {

        private static final Map<String, Boolean> RUNS =
                new ConcurrentHashMap<>();

        private PendingApprovalRunHolder() {
        }

        private static void add(
                String runId) {

            if (runId == null
                    || runId.isBlank()) {

                return;
            }

            RUNS.put(
                    runId,
                    Boolean.TRUE
            );
        }

        private static boolean contains(
                String runId) {

            return RUNS.containsKey(
                    runId
            );
        }

        private static void remove(
                String runId) {

            RUNS.remove(
                    runId
            );
        }
    }
}