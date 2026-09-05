package kr.co.goms.gomsbook.ai.api.agent.bridge;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import kr.co.goms.gomsbook.ai.agent.AgentExecutor;
import kr.co.goms.gomsbook.ai.agent.AgentRequest;
import kr.co.goms.gomsbook.ai.agent.AgentResponse;
import kr.co.goms.gomsbook.ai.agent.AgentToolResultListener;
import kr.co.goms.gomsbook.ai.conversation.model.AiConversationMessageRole;
import kr.co.goms.gomsbook.ai.conversation.model.ConversationHistoryMessage;
import kr.co.goms.gomsbook.ai.llm.LlmMessage;

import java.util.function.Consumer;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

@Component
public class DefaultAgentEngineBridge implements AgentEngineBridge {

    private final AgentExecutor agentExecutor;

    public DefaultAgentEngineBridge(AgentExecutor agentExecutor) {
        this.agentExecutor = Objects.requireNonNull(agentExecutor, "agentExecutor must not be null");
    }

    @Override
    public String generatePreview(String runId, String conversationId, String message) {

        requireText(runId, "runId");
        requireText(conversationId, "conversationId");
        requireText(message, "message");

        AgentRequest request = AgentRequest.builder().requestId(runId).sessionId(conversationId).instruction(message).toolCallingEnabled(true).validationEnabled(true).build();

        AgentResponse response = agentExecutor.execute(request);

        if (response == null) throw new IllegalStateException("Agent returned null response.");
        if (response.isFailure()) throw new IllegalStateException(createErrorMessage(response));
        if (!response.hasContent()) throw new IllegalStateException("Agent returned empty content.");

        return response.getContent();
    }

    @Override
    public String generate(String runId, String projectId, String conversationId, List<ConversationHistoryMessage> _historyMessages, String message, Consumer<ToolResult> toolResultConsumer) {

        requireText(runId, "runId");
        requireText(projectId, "projectId");
        requireText(conversationId, "conversationId");
        requireText(message, "message");

        List<LlmMessage> llmHistoryMessages = toLlmMessages(_historyMessages);
        
        AgentRequest request = createRequest(runId, projectId, conversationId, message, llmHistoryMessages);

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

    private List<LlmMessage> toLlmMessages(List<ConversationHistoryMessage> _historyMessages) {

        if (_historyMessages == null || _historyMessages.isEmpty()) return List.of();

        return _historyMessages.stream()
                .map(this::toLlmMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    private LlmMessage toLlmMessage(ConversationHistoryMessage message) {

        if (message == null || message.content() == null || message.content().isBlank()) return null;

        if (message.role() == AiConversationMessageRole.USER) return LlmMessage.user(message.content());
        if (message.role() == AiConversationMessageRole.ASSISTANT) return LlmMessage.assistant(message.content());

        return null;
    }
    

    private AgentRequest createRequest(String runId, String projectId, String conversationId, String message, List<LlmMessage> llmHistoryMessages) {

        return AgentRequest.builder()
                .requestId(runId)
                .sessionId(conversationId)
                .attribute("runId", runId)
                .attribute("projectId", projectId)
                .attribute("conversationId", conversationId)
                .instruction(message)
                .messages(llmHistoryMessages)
                .toolCallingEnabled(true)
                .validationEnabled(true)
                .build();
    }
    
    private AgentToolResultListener createToolResultListener(
            String runId,
            Consumer<ToolResult> toolResultConsumer) {

        if (toolResultConsumer == null) {

            return null;
        }

        return result -> {

            if (result == null) {

                return;
            }

            String requestId =
                    result.getRequestId();

            if (requestId != null
                    && !requestId.isBlank()
                    && !runId.equals(
                            requestId
                    )) {

                return;
            }

            toolResultConsumer.accept(
                    result
            );
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