package kr.co.goms.gomsbook.ai.api.agent.approval;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.co.goms.gomsbook.ai.api.agent.AgentRunService;

@RestController
@RequestMapping("/api/agent/runs")
public class AgentApprovalController {

    private final AgentRunService agentRunService;

    public AgentApprovalController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping("/{runId}/approvals/{approvalId}/approve")
    public ResponseEntity<Void> approve(@PathVariable String runId, @PathVariable String approvalId) {
        agentRunService.approve(runId, approvalId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{runId}/approvals/{approvalId}/reject")
    public ResponseEntity<Void> reject(@PathVariable String runId, @PathVariable String approvalId) {
        agentRunService.reject(runId, approvalId);
        return ResponseEntity.ok().build();
    }
}