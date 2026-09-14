/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.gomsbook.ai.api.agent.sse;

import java.util.Objects;

import kr.co.goms.gomsbook.ai.agent.event.AgentEvent;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventListener;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventType;

public final class SseAgentEventListener implements AgentEventListener {

    private final AgentSseEventDispatcher dispatcher;

    public SseAgentEventListener(AgentSseEventDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    @Override
    public void onEvent(AgentEvent event) {

        if (event == null) return;

        if (event.getType() == AgentEventType.APPROVAL_REQUIRED) return;

        System.out.println(
                "[GomsBook AI API] Core Agent Event"
                        + " | runId=" + event.getRunId()
                        + " | type=" + event.getType()
        );

        kr.co.goms.gomsbook.ai.api.agent.AgentEvent apiEvent = convert(event);

        dispatcher.send(apiEvent);
    }

    private kr.co.goms.gomsbook.ai.api.agent.AgentEvent convert(AgentEvent event) {

        return kr.co.goms.gomsbook.ai.api.agent.AgentEvent.builder()
                .runId(event.getRunId())
                .type(convertType(event.getType()))
                .message(event.getMessage())
                .data(event.getData())
                .approvalId(event.getApprovalId())
                .title(event.getTitle())
                .fileName(event.getFileName())
                .content(event.getContent())
                .build();
    }

    private kr.co.goms.gomsbook.ai.api.agent.AgentEventType convertType(AgentEventType type) {

        if (type == null) {
            return kr.co.goms.gomsbook.ai.api.agent.AgentEventType.ASSISTANT_MESSAGE;
        }

        return switch (type) {

            case STARTED ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.AGENT_STARTED;

            case MESSAGE ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.ASSISTANT_MESSAGE;

            case RAG_STARTED ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.RAG_STARTED;

            case RAG_PROGRESS ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.RAG_PROGRESS;

            case RAG_CONTEXT ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.RAG_CONTEXT;

            case RAG_COMPLETED ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.RAG_COMPLETED;

            case APPROVAL_REQUIRED ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.APPROVAL_REQUIRED;

            case COMPLETED ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.AGENT_COMPLETED;

            case ERROR ->
                    kr.co.goms.gomsbook.ai.api.agent.AgentEventType.AGENT_FAILED;

            case THINKING, TOOL_CALLING, TOOL_RESULT ->
                    throw new IllegalArgumentException(
                            "Core AgentEventType cannot be mapped directly to API event: " + type
                    );
        };
    }
}