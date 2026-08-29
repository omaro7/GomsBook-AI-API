package kr.co.goms.gomsbook.ai.api.agent;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentRunRequest {

    @NotBlank(message = "message must not be blank")
    private String message;

}