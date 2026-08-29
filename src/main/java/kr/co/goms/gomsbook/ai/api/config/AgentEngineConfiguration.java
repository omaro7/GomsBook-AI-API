package kr.co.goms.gomsbook.ai.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import kr.co.goms.gomsbook.ai.agent.AgentExecutor;
import kr.co.goms.gomsbook.ai.agent.DefaultAgentExecutor;
import kr.co.goms.gomsbook.ai.json.GsonJsonMapper;
import kr.co.goms.gomsbook.ai.json.JsonMapper;
import kr.co.goms.gomsbook.ai.llm.LlmClient;
import kr.co.goms.gomsbook.ai.llm.model.ChatModelProvider;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaConfiguration;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaLlmClient;
import kr.co.goms.gomsbook.ai.tool.ToolContext;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.ToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolRequest;
import kr.co.goms.gomsbook.ai.tool.ToolResult;

@Configuration
public class AgentEngineConfiguration {

    @Value("${gomsbook.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${gomsbook.ai.ollama.chat-model}")
    private String chatModel;

    @Value("${gomsbook.ai.ollama.embedding-model}")
    private String embeddingModel;

    @Bean
    public OllamaConfiguration ollamaConfiguration() {
        return OllamaConfiguration.builder().baseUrl(ollamaBaseUrl).model(chatModel).chatModel(chatModel).embeddingModel(embeddingModel).build();
    }

    @Bean
    public JsonMapper jsonMapper() {
        return new GsonJsonMapper();
    }

    @Bean
    public LlmClient llmClient(OllamaConfiguration configuration, JsonMapper jsonMapper) {
        return new OllamaLlmClient(configuration, jsonMapper);
    }

    @Bean
    public ChatModelProvider chatModelProvider() {
        return () -> chatModel;
    }

    @Bean
    public ToolDefinitionProvider toolDefinitionProvider() {
        return () -> List.of();
    }

    @Bean
    public ToolExecutor toolExecutor() {

        return new ToolExecutor() {

            @Override
            public ToolResult execute(ToolRequest request, ToolContext context) {
                throw new IllegalStateException("No Agent Tools are registered.");
            }

            @Override
            public boolean canExecute(String toolName) {
                return false;
            }
        };
    }

    @Bean
    public AgentExecutor agentExecutor(LlmClient llmClient, ToolExecutor toolExecutor, ToolDefinitionProvider toolDefinitionProvider, ChatModelProvider chatModelProvider) {
        return new DefaultAgentExecutor(llmClient, toolExecutor, toolDefinitionProvider, chatModelProvider);
    }
}