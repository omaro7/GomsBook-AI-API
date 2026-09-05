package kr.co.goms.gomsbook.ai.api.agent;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent/runs")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> run(@RequestBody AgentRunRequest request) {

        String runId = agentRunService.run(
                request.getProjectId(),
                request.getMessage()
        );

        return ResponseEntity.ok(
                Map.of(
                        "runId",
                        runId
                )
        );
    }

    @GetMapping("/{runId}/events")
    public SseEmitter events(@PathVariable String runId) {
        return agentRunService.subscribe(runId);
    }
}