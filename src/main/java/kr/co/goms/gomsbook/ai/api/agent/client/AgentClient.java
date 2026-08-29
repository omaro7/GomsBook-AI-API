package kr.co.goms.gomsbook.ai.api.agent.client;

public interface AgentClient {

    String generatePreview(String runId, String message);

    void executeApproved(String runId, String approvalId, String action, String fileName, String content);
}