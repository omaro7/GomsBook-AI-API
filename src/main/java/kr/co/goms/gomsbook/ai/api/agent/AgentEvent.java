package kr.co.goms.gomsbook.ai.api.agent;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentEvent {

    private final String runId;

    private final AgentEventType type;

    private final String message;

    private final String toolCallId;

    private final String toolName;

    @Builder.Default
    private final Instant timestamp = Instant.now();

}