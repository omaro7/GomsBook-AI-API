package kr.co.goms.gomsbook.ai.api.agent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.co.goms.gomsbook.ai.agent.approval.AgentApproval;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalService;
import kr.co.goms.gomsbook.ai.api.agent.bridge.AgentEngineBridge;
import kr.co.goms.gomsbook.ai.api.agent.sse.AgentSseEventDispatcher;
import kr.co.goms.gomsbook.ai.tool.ToolResult;
import kr.co.goms.gomsbook.ai.conversation.service.ConversationService;

import java.util.List;
import kr.co.goms.gomsbook.ai.conversation.model.ConversationHistoryMessage;

@Service
public class AgentRunService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);

    private final AgentEngineBridge agentEngineBridge;

    private final AgentSseEventDispatcher sseDispatcher;

    private final AgentApprovalService approvalService;

    private final AgentApprovalExecutor approvalExecutor;

    private final Map<String, PendingAgentRun> pendingRuns = new ConcurrentHashMap<>();

    private final Map<String, RunTraceContext> runContexts = new ConcurrentHashMap<>();

    private final Map<String, Boolean> activeRuns = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ConversationService conversationService;
    
    public AgentRunService(
            AgentEngineBridge agentEngineBridge,
            AgentSseEventDispatcher sseDispatcher,
            AgentApprovalService approvalService,
            AgentApprovalExecutor approvalExecutor,
            ConversationService conversationService
    ) {

        this.agentEngineBridge = agentEngineBridge;
        this.sseDispatcher = sseDispatcher;
        this.approvalService = approvalService;
        this.approvalExecutor = approvalExecutor;
        this.conversationService = conversationService;
    }

    public String run(
            String projectId,
            String conversationId,
            String message) {

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank.");
        }

        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be blank.");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank.");
        }

        String runId = UUID.randomUUID().toString();

        String normalizedProjectId = projectId.trim();
        String normalizedConversationId = conversationId.trim();
        String normalizedMessage = message.trim();

        conversationService.getOrCreateConversation(
                normalizedProjectId,
                normalizedConversationId
        );

        conversationService.addUserMessage(
                normalizedConversationId,
                runId,
                normalizedMessage
        );

        pendingRuns.put(
                runId,
                new PendingAgentRun(
                        normalizedProjectId,
                        normalizedConversationId,
                        normalizedMessage
                )
        );

        runContexts.put(
                runId,
                new RunTraceContext(
                        normalizedProjectId,
                        normalizedConversationId
                )
        );

        activeRuns.put(runId, Boolean.TRUE);

        log.info(
                "Agent run created | projectId={} | conversationId={} | runId={}",
                normalizedProjectId,
                normalizedConversationId,
                runId
        );

        return runId;
    }

    public SseEmitter subscribe(String runId) {

        SseEmitter emitter = sseDispatcher.subscribe(runId);

        PendingAgentRun pendingRun = pendingRuns.remove(runId);

        if (pendingRun != null) {

            executor.submit(
                    () -> withMdc(
                            runId,
                            () -> executeAgent(
                                    runId,
                                    pendingRun.projectId(),
                                    pendingRun.message()
                            )
                    )
            );
        }

        return emitter;
    }

    public void approve(String runId, String approvalId) {
        withMdc(runId, () -> approveInternal(runId, approvalId));
    }

    public void reject(String runId, String approvalId) {
        withMdc(runId, () -> rejectInternal(runId, approvalId));
    }

    private void approveInternal(String runId, String approvalId) {

        AgentApproval approval = approvalService.get(approvalId);

        validateRun(runId, approval);

        approvalService.approve(approvalId);

        ApprovalOperation operation = resolveApprovalOperation(approval);

        log.info(
                "Approval approved | approvalId={} | action={} | operation={}",
                approval.getApprovalId(),
                approval.getAction(),
                operation
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_APPROVED)
                        .message("사용자가 작업을 승인했습니다.")
                        .approvalId(approval.getApprovalId())
                        .title(approval.getTitle())
                        .fileName(approval.getFileName())
                        .build()
        );

        executor.submit(
                () -> withMdc(
                        runId,
                        () -> executeApprovedAction(approval)
                )
        );
    }

    private void rejectInternal(String runId, String approvalId) {

        AgentApproval approval = approvalService.get(approvalId);

        validateRun(runId, approval);

        approvalService.reject(approvalId);

        ApprovalOperation operation = resolveApprovalOperation(approval);

        log.info(
                "Approval rejected | approvalId={} | action={} | operation={}",
                approval.getApprovalId(),
                approval.getAction(),
                operation
        );

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_REJECTED)
                        .message("사용자가 작업을 취소했습니다.")
                        .approvalId(approval.getApprovalId())
                        .title(approval.getTitle())
                        .fileName(approval.getFileName())
                        .build()
        );

        PendingApprovalRunHolder.remove(runId, approvalId);

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.ASSISTANT_MESSAGE)
                        .message(createCancellationMessage(approval, operation))
                        .build()
        );

        completeIfNoPendingApproval(runId);
    }

    private void executeAgent(
        String runId,
        String projectId,
        String message) {

	    try {
	
	        send(
	                AgentEvent.builder()
	                        .runId(runId)
	                        .type(AgentEventType.AGENT_STARTED)
	                        .message("Agent 실행을 시작합니다.")
	                        .build()
	        );
	
	        RunTraceContext context = runContexts.get(runId);
	
	        if (context == null) throw new IllegalStateException("Run trace context not found: " + runId);
	
	        List<ConversationHistoryMessage> historyMessages =
	                conversationService.getConversationHistory(
	                        context.conversationId(),
	                        runId
	                );
	
	        System.out.println(
	                "[GomsBook AI API] Conversation History"
	                        + " | conversationId=" + context.conversationId()
	                        + " | messageCount=" + historyMessages.size()
	        );
	
	        String response =
	                agentEngineBridge.generate(
	                        runId,
	                        projectId,
	                        context.conversationId(),
	                        historyMessages,
	                        message,
	                        toolResult -> handleToolResult(
	                                runId,
	                                toolResult
	                        )
	                );
	
	        if (response != null && !response.isBlank()) {
	
	            conversationService.addAssistantMessage(
	                    context.conversationId(),
	                    runId,
	                    response
	            );
	
	            send(
	                    AgentEvent.builder()
	                            .runId(runId)
	                            .type(AgentEventType.ASSISTANT_MESSAGE)
	                            .message(response)
	                            .build()
	            );
	        }
	
	        if (hasPendingApproval(runId)) {
	
	            System.out.println(
	                    "[GomsBook AI API] Agent waiting for approval"
	                            + " | runId=" + runId
	                            + " | pendingApprovalCount=" + PendingApprovalRunHolder.count(runId)
	            );
	
	            return;
	        }
	
	        completeRun(runId);
	
	    } catch (Exception exception) {
	
	        fail(
	                runId,
	                exception
	        );
	    }
	}
    
    private void handleToolResult(String runId, ToolResult toolResult) {

        if (toolResult == null) return;

        AgentEventType type = toolResult.hasError()
                ? AgentEventType.TOOL_FAILED
                : AgentEventType.TOOL_COMPLETED;

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(type)
                        .message(resolveToolMessage(toolResult))
                        .toolCallId(toolResult.getToolCallId())
                        .toolName(toolResult.getToolName())
                        .data(toolResult.hasData() ? toolResult.getData() : null)
                        .build()
        );

        if (!isApprovalRequired(toolResult)) return;

        String approvalId = getDataString(toolResult, "approvalId");

        String title = getDataString(toolResult, "title");

        String message = getDataString(toolResult, "message");

        String fileName = getDataString(toolResult, "fileName");

        String content = getDataString(toolResult, "content");

        if (approvalId == null || approvalId.isBlank()) {
            throw new IllegalStateException("Approval ToolResult에 approvalId가 없습니다.");
        }

        PendingApprovalRunHolder.add(runId, approvalId);

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.APPROVAL_REQUIRED)
                        .message(message)
                        .toolName(toolResult.getToolName())
                        .approvalId(approvalId)
                        .title(title)
                        .fileName(fileName)
                        .content(content)
                        .build()
        );

        log.info(
                "Approval required | toolName={} | approvalId={} | fileName={}",
                toolResult.getToolName(),
                approvalId,
                fileName
        );
    }

    private void executeApprovedAction(AgentApproval approval) {

        ApprovalOperation operation = resolveApprovalOperation(approval);

        try {

            log.info(
                    "Approved action started | approvalId={} | action={} | operation={} | fileName={}",
                    approval.getApprovalId(),
                    approval.getAction(),
                    operation,
                    approval.getFileName()
            );

            send(
                    AgentEvent.builder()
                            .runId(approval.getRunId())
                            .type(AgentEventType.TOOL_STARTED)
                            .message(createStartMessage(approval, operation))
                            .toolName(approval.getAction())
                            .approvalId(approval.getApprovalId())
                            .title(approval.getTitle())
                            .fileName(approval.getFileName())
                            .build()
            );

            approvalExecutor.execute(approval);

            send(
                    AgentEvent.builder()
                            .runId(approval.getRunId())
                            .type(AgentEventType.TOOL_COMPLETED)
                            .message(createToolCompletionMessage(approval, operation))
                            .toolName(approval.getAction())
                            .approvalId(approval.getApprovalId())
                            .title(approval.getTitle())
                            .fileName(approval.getFileName())
                            .build()
            );

            send(
                    AgentEvent.builder()
                            .runId(approval.getRunId())
                            .type(AgentEventType.ASSISTANT_MESSAGE)
                            .message(createCompletionMessage(approval, operation))
                            .build()
            );

            PendingApprovalRunHolder.remove(
                    approval.getRunId(),
                    approval.getApprovalId()
            );

            log.info(
                    "Approved action completed | approvalId={} | action={} | operation={} | fileName={}",
                    approval.getApprovalId(),
                    approval.getAction(),
                    operation,
                    approval.getFileName()
            );

            completeIfNoPendingApproval(approval.getRunId());

        } catch (Exception exception) {

            log.error(
                    "Approved action failed | approvalId={} | action={} | operation={} | error={}",
                    approval.getApprovalId(),
                    approval.getAction(),
                    operation,
                    resolveErrorMessage(exception),
                    exception
            );

            sendSafely(
                    AgentEvent.builder()
                            .runId(approval.getRunId())
                            .type(AgentEventType.TOOL_FAILED)
                            .message(resolveErrorMessage(exception))
                            .toolName(approval.getAction())
                            .approvalId(approval.getApprovalId())
                            .title(approval.getTitle())
                            .fileName(approval.getFileName())
                            .build()
            );

            sendSafely(
                    AgentEvent.builder()
                            .runId(approval.getRunId())
                            .type(AgentEventType.ASSISTANT_MESSAGE)
                            .message(createFailureMessage(approval, operation, exception))
                            .build()
            );

            PendingApprovalRunHolder.remove(
                    approval.getRunId(),
                    approval.getApprovalId()
            );

            completeIfNoPendingApproval(approval.getRunId());
        }
    }

    private boolean isApprovalRequired(ToolResult toolResult) {

        if (toolResult == null || !toolResult.hasData() || toolResult.getData() == null) return false;

        Object value = toolResult.getData().get("approvalRequired");

        if (value instanceof Boolean) return ((Boolean) value).booleanValue();

        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean hasPendingApproval(String runId) {
        return PendingApprovalRunHolder.contains(runId);
    }

    private void validateRun(String runId, AgentApproval approval) {

        if (runId == null || runId.isBlank()) throw new IllegalArgumentException("runId must not be blank.");

        if (approval == null) throw new IllegalArgumentException("approval must not be null.");

        if (!runId.equals(approval.getRunId())) {
            throw new IllegalArgumentException(
                    "Approval run ID does not match."
                            + " requestRunId="
                            + runId
                            + ", approvalRunId="
                            + approval.getRunId()
            );
        }

        log.info(
                "Approval run validation | status=VALID | approvalId={}",
                approval.getApprovalId()
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

    private String getDataString(ToolResult toolResult, String name) {

        if (toolResult == null || !toolResult.hasData() || toolResult.getData() == null) return null;

        Object value = toolResult.getData().get(name);

        return value == null ? null : String.valueOf(value);
    }

    private void send(AgentEvent event) {
        sseDispatcher.send(event);
    }

    private void sendSafely(AgentEvent event) {

        try {

            send(event);

        } catch (Exception exception) {

            log.warn(
                    "Failed to send SSE event | error={}",
                    exception.getMessage()
            );
        }
    }

    private void fail(String runId, Exception exception) {

        if (!markRunCompleted(runId)) return;

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.AGENT_FAILED)
                        .message(resolveErrorMessage(exception))
                        .build()
        );

        closeRun(runId);
    }

    private String resolveErrorMessage(Exception exception) {

        if (exception == null) return "Agent 실행 중 오류가 발생했습니다.";

        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? "Agent 실행 중 오류가 발생했습니다."
                : message;
    }

    private void completeIfNoPendingApproval(String runId) {

        if (hasPendingApproval(runId)) {

            log.info(
                    "Agent still waiting for approval | pendingApprovalCount={}",
                    PendingApprovalRunHolder.count(runId)
            );

            return;
        }

        completeRun(runId);
    }

    private void completeRun(String runId) {

        if (!markRunCompleted(runId)) return;

        log.info("Agent execution completed");

        sendSafely(
                AgentEvent.builder()
                        .runId(runId)
                        .type(AgentEventType.AGENT_COMPLETED)
                        .build()
        );

        closeRun(runId);
    }

    private boolean markRunCompleted(String runId) {
        return activeRuns.remove(runId, Boolean.TRUE);
    }

    private void closeRun(String runId) {

        sseDispatcher.complete(runId);

        pendingRuns.remove(runId);

        PendingApprovalRunHolder.remove(runId);

        runContexts.remove(runId);
    }

    private void withMdc(String runId, Runnable runnable) {

        RunTraceContext context = runContexts.get(runId);

        try {

            if (context != null) {
                MDC.put("projectId", context.projectId());
                MDC.put("conversationId", context.conversationId());
            }

            MDC.put("runId", runId);

            runnable.run();

        } finally {

            MDC.remove("projectId");
            MDC.remove("conversationId");
            MDC.remove("runId");
        }
    }

    private ApprovalOperation resolveApprovalOperation(AgentApproval approval) {

        if (approval == null || approval.getAction() == null) return ApprovalOperation.UNKNOWN;

        String action = approval.getAction().trim().toLowerCase();

        if (action.contains("create_")) return ApprovalOperation.CREATE;

        if (action.contains("update_")) return ApprovalOperation.UPDATE;

        if (action.contains("delete_")) return ApprovalOperation.DELETE;

        return ApprovalOperation.UNKNOWN;
    }

    private String createStartMessage(
            AgentApproval approval,
            ApprovalOperation operation) {

        String fileName = approval.getFileName();

        if (operation == ApprovalOperation.CREATE) return fileName + " 생성을 시작합니다.";

        if (operation == ApprovalOperation.UPDATE) return fileName + " 수정을 시작합니다.";

        if (operation == ApprovalOperation.DELETE) return fileName + " 삭제를 시작합니다.";

        return fileName + " 처리를 시작합니다.";
    }

    private String createToolCompletionMessage(
            AgentApproval approval,
            ApprovalOperation operation) {

        String fileName = approval.getFileName();

        if (operation == ApprovalOperation.CREATE) return fileName + " 생성이 완료되었습니다.";

        if (operation == ApprovalOperation.UPDATE) return fileName + " 수정이 완료되었습니다.";

        if (operation == ApprovalOperation.DELETE) return fileName + " 삭제가 완료되었습니다.";

        return fileName + " 처리가 완료되었습니다.";
    }

    private String createCompletionMessage(
            AgentApproval approval,
            ApprovalOperation operation) {

        String fileName = approval.getFileName();

        if (operation == ApprovalOperation.CREATE) return fileName + "을 생성했습니다.";

        if (operation == ApprovalOperation.UPDATE) return fileName + "을 수정했습니다.";

        if (operation == ApprovalOperation.DELETE) return fileName + "을 삭제했습니다.";

        return fileName + " 작업을 완료했습니다.";
    }

    private String createCancellationMessage(
            AgentApproval approval,
            ApprovalOperation operation) {

        String fileName = approval.getFileName();

        if (operation == ApprovalOperation.CREATE) return fileName + " 생성을 취소했습니다.";

        if (operation == ApprovalOperation.UPDATE) return fileName + " 수정을 취소했습니다.";

        if (operation == ApprovalOperation.DELETE) return fileName + " 삭제를 취소했습니다.";

        return fileName + " 작업을 취소했습니다.";
    }

    private String createFailureMessage(
            AgentApproval approval,
            ApprovalOperation operation,
            Exception exception) {

        String fileName = approval.getFileName();

        return fileName
                + " "
                + operation.getLabel()
                + "에 실패했습니다: "
                + resolveErrorMessage(exception);
    }

    private static final class PendingApprovalRunHolder {

        private static final Map<String, Map<String, Boolean>> RUNS = new ConcurrentHashMap<>();

        private PendingApprovalRunHolder() {
        }

        private static void add(String runId, String approvalId) {

            if (runId == null || runId.isBlank() || approvalId == null || approvalId.isBlank()) return;

            RUNS.computeIfAbsent(
                    runId,
                    key -> new ConcurrentHashMap<>()
            ).put(
                    approvalId,
                    Boolean.TRUE
            );
        }

        private static boolean contains(String runId) {

            Map<String, Boolean> approvals = RUNS.get(runId);

            return approvals != null && !approvals.isEmpty();
        }

        private static int count(String runId) {

            Map<String, Boolean> approvals = RUNS.get(runId);

            return approvals == null ? 0 : approvals.size();
        }

        private static void remove(String runId, String approvalId) {

            Map<String, Boolean> approvals = RUNS.get(runId);

            if (approvals == null) return;

            approvals.remove(approvalId);

            if (approvals.isEmpty()) RUNS.remove(runId, approvals);
        }

        private static void remove(String runId) {
            RUNS.remove(runId);
        }
    }

    private record PendingAgentRun(
            String projectId,
            String conversationId,
            String message) {
    }

    private record RunTraceContext(
            String projectId,
            String conversationId) {
    }

    private enum ApprovalOperation {

        CREATE("생성"),
        UPDATE("수정"),
        DELETE("삭제"),
        UNKNOWN("처리");

        private final String label;

        ApprovalOperation(String label) {
            this.label = label;
        }

        private String getLabel() {
            return label;
        }
    }
}