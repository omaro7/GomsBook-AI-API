package kr.co.goms.gomsbook.ai.api.agent.sse;

import java.util.Objects;

import kr.co.goms.gomsbook.ai.agent.event.AgentEvent;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventListener;

public final class SseAgentEventListener
        implements AgentEventListener {

    private final AgentSseEventDispatcher dispatcher;

    public SseAgentEventListener(
            AgentSseEventDispatcher dispatcher) {

        this.dispatcher =
                Objects.requireNonNull(
                        dispatcher,
                        "dispatcher must not be null"
                );
    }

    @Override
    public void onEvent(
            AgentEvent event) {

        if (event == null) {

            return;
        }

        if (event.getType() == kr.co.goms.gomsbook.ai.agent.event.AgentEventType.APPROVAL_REQUIRED) {

            return;
        }
        
        System.out.println(
                "[GomsBook AI API] Core Agent Event"
                        + " | runId="
                        + event.getRunId()
                        + " | type="
                        + event.getType()
        );

        kr.co.goms.gomsbook.ai.api.agent.AgentEvent apiEvent =
                convert(
                        event
                );

        dispatcher.send(
                apiEvent
        );
    }

    private kr.co.goms.gomsbook.ai.api.agent.AgentEvent convert(
            AgentEvent event) {

        return kr.co.goms.gomsbook.ai.api.agent.AgentEvent.builder()
                .runId(
                        event.getRunId()
                )
                .type(
                        convertType(
                                event
                                        .getType()
                                        .name()
                        )
                )
                .message(
                        event.getMessage()
                )
                .approvalId(
                        event.getApprovalId()
                )
                .title(
                        event.getTitle()
                )
                .fileName(
                        event.getFileName()
                )
                .content(
                        event.getContent()
                )
                .build();
    }

    private kr.co.goms.gomsbook.ai.api.agent.AgentEventType convertType(
            String type) {

        if (type == null
                || type.isBlank()) {

            return kr.co.goms.gomsbook.ai.api.agent.AgentEventType.ASSISTANT_MESSAGE;
        }

        if ("MESSAGE".equals(
                type
        )) {

            return kr.co.goms.gomsbook.ai.api.agent.AgentEventType.ASSISTANT_MESSAGE;
        }

        if ("APPROVAL_REQUIRED".equals(
                type
        )) {

            return kr.co.goms.gomsbook.ai.api.agent.AgentEventType.APPROVAL_REQUIRED;
        }

        throw new IllegalArgumentException(
                "Unsupported Core AgentEventType: "
                        + type
        );
    }
}