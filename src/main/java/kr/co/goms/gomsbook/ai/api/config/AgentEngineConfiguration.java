package kr.co.goms.gomsbook.ai.api.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import kr.co.goms.gomsbook.ai.accessibility.validation.AccessibilityValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.DefaultAccessibilityValidator;
import kr.co.goms.gomsbook.ai.agent.AgentExecutor;
import kr.co.goms.gomsbook.ai.agent.DefaultAgentExecutor;
import kr.co.goms.gomsbook.ai.epub.service.EpubCheckRunner;
import kr.co.goms.gomsbook.ai.epub.service.PublishDirectoryProvider;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckRunnerValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckValidator;
import kr.co.goms.gomsbook.ai.json.GsonJsonMapper;
import kr.co.goms.gomsbook.ai.json.JsonMapper;
import kr.co.goms.gomsbook.ai.llm.LlmClient;
import kr.co.goms.gomsbook.ai.llm.model.ChatModelProvider;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaConfiguration;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaLlmClient;
import kr.co.goms.gomsbook.ai.project.CurrentProjectProvider;
import kr.co.goms.gomsbook.ai.project.DefaultCurrentProjectProvider;
import kr.co.goms.gomsbook.ai.tool.AgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultAgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.DefaultToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.ToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolRegistry;

import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalService;
import kr.co.goms.gomsbook.ai.agent.approval.DefaultAgentApprovalService;
import kr.co.goms.gomsbook.ai.agent.event.DefaultAgentEventPublisher;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventPublisher;
import kr.co.goms.gomsbook.ai.api.agent.sse.AgentSseEventDispatcher;
import kr.co.goms.gomsbook.ai.api.agent.sse.SseAgentEventListener;

import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.DefaultAgentApprovalExecutor;


@Configuration
public class AgentEngineConfiguration {

    @Value("${gomsbook.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${gomsbook.ai.ollama.chat-model}")
    private String chatModel;

    @Value("${gomsbook.ai.ollama.embedding-model}")
    private String embeddingModel;

    @Value("${gomsbook.ai.project-root}")
    private String projectRoot;

    @Value("${gomsbook.ai.publish-directory}")
    private String publishDirectory;

    @Value("${gomsbook.ai.epubcheck.directory}")
    private String epubCheckDirectory;

    @Value("${gomsbook.ai.epubcheck.version}")
    private String epubCheckVersion;

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
    public CurrentProjectProvider currentProjectProvider() {
        return new DefaultCurrentProjectProvider(() -> Path.of(projectRoot));
    }

    @Bean
    public PublishDirectoryProvider publishDirectoryProvider() {
        return () -> Path.of(publishDirectory);
    }

    @Bean
    public EpubCheckRunner epubCheckRunner() {
        return new EpubCheckRunner(Path.of(epubCheckDirectory), epubCheckVersion);
    }

    @Bean
    public EpubCheckValidator epubCheckValidator(EpubCheckRunner epubCheckRunner) {
        return new EpubCheckRunnerValidator(epubCheckRunner, epubCheckVersion);
    }

    @Bean
    public AccessibilityValidator accessibilityValidator() {
        return new DefaultAccessibilityValidator(List.of());
    }

    @Bean
    public AgentToolRegistrar agentToolRegistrar(CurrentProjectProvider currentProjectProvider, PublishDirectoryProvider publishDirectoryProvider, EpubCheckValidator epubCheckValidator, 
    		AccessibilityValidator accessibilityValidator,
    		AgentApprovalService approvalService,
            AgentEventPublisher eventPublisher) {
        return new DefaultAgentToolRegistrar(currentProjectProvider, publishDirectoryProvider, epubCheckValidator, accessibilityValidator, approvalService, eventPublisher);
    }

    @Bean
    public ToolRegistry toolRegistry(AgentToolRegistrar agentToolRegistrar) {
        ToolRegistry registry = new ToolRegistry();
        agentToolRegistrar.registerTools(registry);
        System.out.println("[GomsBook AI API] Registered tools = " + registry.getToolNames());
        return registry;
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
    
    @Bean
    public AgentApprovalService agentApprovalService() {

        return new DefaultAgentApprovalService();
    }

    @Bean
    public AgentApprovalExecutor agentApprovalExecutor(
            CurrentProjectProvider currentProjectProvider) {

        return new DefaultAgentApprovalExecutor(
                currentProjectProvider
        );
    }
    
    @Bean
    public DefaultAgentEventPublisher agentEventPublisher(
            AgentSseEventDispatcher dispatcher) {

        DefaultAgentEventPublisher publisher =
                new DefaultAgentEventPublisher();

        publisher.addListener(
                new SseAgentEventListener(
                        dispatcher
                )
        );

        return publisher;
    }

}