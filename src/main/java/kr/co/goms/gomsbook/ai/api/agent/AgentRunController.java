package kr.co.goms.gomsbook.ai.api.agent;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(
            AgentRunService agentRunService) {

        this.agentRunService = agentRunService;

    }

    @PostMapping("/run")
    public AgentRunResponse run(
            @Valid @RequestBody AgentRunRequest request) {

        String runId =
                agentRunService.run(
                        request.getMessage());

        return new AgentRunResponse(runId);

    }

    @GetMapping(
            value = "/runs/{runId}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(
            @PathVariable String runId) {

        return agentRunService.subscribe(runId);

    }

}