/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.gomsbook.ai.api.agent.prompt.rule;

/**
 * Agent Tool 실행 후 적용할 Prompt 규칙입니다.
 *
 * <p>
 * 하나의 Prompt Rule은 하나 이상의 Tool에 적용할 수 있습니다.
 * </p>
 */
public interface AgentPromptRule {

    /**
     * 이 Prompt Rule이 적용될 Tool 이름을 반환합니다.
     *
     * @return Tool 이름 
     */
    String getToolName();

    /**
     * Tool 실행 후 LLM에 전달할 Prompt 규칙을 반환합니다.
     *
     * @return Prompt 규칙
     */
    String getPrompt();
}