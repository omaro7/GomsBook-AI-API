package kr.co.goms.gomsbook.ai.api.agent;

import java.util.List;

public record AgentConfigResponse(
        String defaultAgent,
        String defaultModel,
        boolean ragEnabled,
        boolean mcpEnabled,
        List<AgentConfigOption> agents,
        List<AgentConfigOption> models) {
}