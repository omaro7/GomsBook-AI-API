package kr.co.goms.gomsbook.ai.api.agent;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * agent와 model을 가지고 오는 controller 
 */

@RestController
@RequestMapping("/api/agent")
public class AgentConfigController {

    @GetMapping("/config")
    public ResponseEntity<AgentConfigResponse> getConfig() {

        AgentConfigResponse response =
                new AgentConfigResponse(
                        "Default Agent",
                        "gemma4:31b-cloud",
                        false,
                        false,
                        List.of(
                                new AgentConfigOption(
                                        "Default Agent",
                                        "Default Agent")),
                        List.of(
                                new AgentConfigOption(
                                        "gemma4:31b-cloud",
                                        "gemma4:31b-cloud")));

        return ResponseEntity.ok(response);
    }
}