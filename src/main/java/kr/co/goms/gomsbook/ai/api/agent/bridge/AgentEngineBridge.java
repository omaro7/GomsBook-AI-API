package kr.co.goms.gomsbook.ai.api.agent.bridge;

import java.util.function.Consumer;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

public interface AgentEngineBridge {

    String generatePreview(String runId, String message);
    String generate(String runId, String message, Consumer<ToolResult> toolResultConsumer);
    void executeApproved(String runId, String approvalId, String action, String fileName, String content);
    
    
}