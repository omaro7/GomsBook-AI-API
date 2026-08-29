package kr.co.goms.gomsbook.ai.api.agent;

public enum AgentEventType {

    AGENT_STARTED,

    ASSISTANT_MESSAGE,

    RAG_STARTED,

    RAG_CONTEXT,

    RAG_COMPLETED,

    TOOL_STARTED,

    TOOL_COMPLETED,

    TOOL_FAILED,

    APPROVAL_REQUIRED,

    AGENT_COMPLETED,

    AGENT_FAILED

}