package kr.co.goms.gomsbook.ai.api.agent;

/**
 * GomsBook-AI-API
 * API/SSE를 통해 클라이언트에 전달되는 공개 Agent 이벤트 유형입니다.
 * Core AgentEventType과 동일한 enum이 아니며 API 전송 계약을 정의합니다.
*/
public enum AgentEventType {

    AGENT_STARTED,

    ASSISTANT_MESSAGE,

    RAG_STARTED,
    
    RAG_PROGRESS,

    RAG_CONTEXT,

    RAG_COMPLETED,

    TOOL_STARTED,

    TOOL_COMPLETED,

    TOOL_FAILED,

    APPROVAL_REQUIRED,

    APPROVAL_APPROVED,

    APPROVAL_REJECTED,

    AGENT_COMPLETED,

    AGENT_FAILED
}