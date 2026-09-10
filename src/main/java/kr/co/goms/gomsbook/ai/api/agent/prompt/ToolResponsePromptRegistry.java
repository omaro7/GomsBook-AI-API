/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.gomsbook.ai.api.agent.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.gomsbook.ai.agent.prompt.ToolResponsePromptResolver;
import kr.co.goms.gomsbook.ai.api.agent.prompt.rule.AgentPromptRule;

/**
 * Tool별 추가 Prompt Rule을 관리합니다.
 */
public final class ToolResponsePromptRegistry implements ToolResponsePromptResolver {

    private final Map<String, AgentPromptRule> rules;

    public ToolResponsePromptRegistry(List<AgentPromptRule> promptRules) {

        this.rules = new LinkedHashMap<>();

        if (promptRules == null || promptRules.isEmpty()) return;

        for (AgentPromptRule promptRule : promptRules) register(promptRule);
    }

    private void register(AgentPromptRule promptRule) {

        Objects.requireNonNull(promptRule, "promptRule must not be null");

        String toolName = promptRule.getToolName();

        if (toolName == null || toolName.isBlank()) throw new IllegalArgumentException("Prompt Rule toolName must not be blank.");

        if (rules.containsKey(toolName)) throw new IllegalStateException("Duplicate Prompt Rule registration. toolName=" + toolName);

        rules.put(toolName, promptRule);
    }

    @Override
    public String resolve(String toolName) {

        if (toolName == null || toolName.isBlank()) return null;

        AgentPromptRule promptRule = rules.get(toolName);

        if (promptRule == null) return null;

        String prompt = promptRule.getPrompt();

        return prompt == null || prompt.isBlank() ? null : prompt.trim();
    }

    public boolean contains(String toolName) {
        return toolName != null && !toolName.isBlank() && rules.containsKey(toolName);
    }

    public int size() {
        return rules.size();
    }
}