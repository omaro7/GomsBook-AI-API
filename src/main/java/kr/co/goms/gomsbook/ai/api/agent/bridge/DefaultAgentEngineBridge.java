package kr.co.goms.gomsbook.ai.api.agent.bridge;

import java.util.Objects;

import org.springframework.stereotype.Component;

import kr.co.goms.gomsbook.ai.agent.AgentExecutor;
import kr.co.goms.gomsbook.ai.agent.AgentRequest;
import kr.co.goms.gomsbook.ai.agent.AgentResponse;
import kr.co.goms.gomsbook.ai.agent.AgentToolResultListener;

import java.util.function.Consumer;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

@Component
public class DefaultAgentEngineBridge implements AgentEngineBridge {

    private final AgentExecutor agentExecutor;

    public DefaultAgentEngineBridge(AgentExecutor agentExecutor) {
        this.agentExecutor = Objects.requireNonNull(agentExecutor, "agentExecutor must not be null");
    }

    @Override
    public String generatePreview(String runId, String message) {

        requireText(runId, "runId");
        requireText(message, "message");

        AgentRequest request = AgentRequest.builder().requestId(runId).sessionId(runId).instruction(message).toolCallingEnabled(true).validationEnabled(true).build();

        AgentResponse response = agentExecutor.execute(request);

        if (response == null) throw new IllegalStateException("Agent returned null response.");
        if (response.isFailure()) throw new IllegalStateException(createErrorMessage(response));
        if (!response.hasContent()) throw new IllegalStateException("Agent returned empty content.");

        return response.getContent();
    }

    @Override
    public String generate(String runId, String message, Consumer<ToolResult> toolResultConsumer) {

        requireText(runId, "runId");
        requireText(message, "message");

        AgentRequest request = createRequest(runId, message);

        AgentToolResultListener listener = createToolResultListener(runId, toolResultConsumer);

        if (listener != null) agentExecutor.addToolResultListener(listener);

        try {

            AgentResponse response = agentExecutor.execute(request);

            validateResponse(response);

            return response.getContent();

        } finally {

            if (listener != null) agentExecutor.removeToolResultListener(listener);
        }
    }
    
    @Override
    public void executeApproved(String runId, String approvalId, String action, String fileName, String content) {

        requireText(runId, "runId");
        requireText(approvalId, "approvalId");
        requireText(action, "action");
        requireText(fileName, "fileName");
        requireText(content, "content");

        throw new UnsupportedOperationException("Approved EPUB execution is not connected yet.");
    }


    private AgentRequest createRequest(String runId, String message) {

        return AgentRequest.builder()
                .requestId(runId)
                .sessionId(runId)
                .instruction(message)
                .toolCallingEnabled(true)
                .validationEnabled(true)
                .build();
    }

    
    private AgentToolResultListener createToolResultListener(String runId, Consumer<ToolResult> toolResultConsumer) {

        if (toolResultConsumer == null) return null;

        return result -> {

            if (result == null) return;

            if (!runId.equals(result.getRequestId())) return;

            toolResultConsumer.accept(result);
        };
    }

    private static void validateResponse(AgentResponse response) {

        if (response == null) throw new IllegalStateException("Agent returned null response.");

        if (response.isFailure()) throw new IllegalStateException(createErrorMessage(response));

        if (!response.hasContent()) throw new IllegalStateException("Agent returned empty content.");
    }
    
    private static String createErrorMessage(AgentResponse response) {
        String errorMessage = response.getErrorMessage();
        return errorMessage == null || errorMessage.isBlank() ? "Agent execution failed." : "Agent execution failed: " + errorMessage;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}