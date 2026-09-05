package kr.co.goms.gomsbook.ai.api.agent.bridge;

import java.util.List;
import java.util.function.Consumer;

import kr.co.goms.gomsbook.ai.conversation.model.ConversationHistoryMessage;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

public interface AgentEngineBridge {

    String generatePreview(String runId, String conversationId, String message);
    String generate(String runId, String projectId, String conversationId,  List<ConversationHistoryMessage> historyMessages, String message, Consumer<ToolResult> toolResultConsumer);
    void executeApproved(String runId, String approvalId, String action, String fileName, String content);
    
    
}