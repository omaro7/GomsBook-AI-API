package kr.co.goms.gomsbook.ai.api.config;

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
import kr.co.goms.gomsbook.ai.tool.AgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultAgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.DefaultToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.ToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolRegistry;

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
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();
        new DefaultAgentToolRegistrar().registerTools(registry);
        System.out.println("[GomsBook AI API] Registered tools = " + registry.getToolNames());
        return registry;
    }
    @Bean
    public AgentToolRegistrar agentToolRegistrar() {
        return new DefaultAgentToolRegistrar();
    }

    @Bean
    public ToolDefinitionMapper toolDefinitionMapper() {
        return new DefaultToolDefinitionMapper();
    }

    @Bean
    public ToolDefinitionProvider toolDefinitionProvider(ToolRegistry toolRegistry, ToolDefinitionMapper toolDefinitionMapper) {
        return new DefaultToolDefinitionProvider(toolRegistry, toolDefinitionMapper);
    }

    @Bean
    public ToolExecutor toolExecutor(ToolRegistry toolRegistry) {
        return new DefaultToolExecutor(toolRegistry);
    }

    @Bean
    public AgentExecutor agentExecutor(LlmClient llmClient, ToolExecutor toolExecutor, ToolDefinitionProvider toolDefinitionProvider, ChatModelProvider chatModelProvider) {
        return new DefaultAgentExecutor(llmClient, toolExecutor, toolDefinitionProvider, chatModelProvider);
    }
}