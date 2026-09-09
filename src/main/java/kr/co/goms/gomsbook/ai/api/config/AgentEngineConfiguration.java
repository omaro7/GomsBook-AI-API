/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.gomsbook.ai.api.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.gson.Gson;

import kr.co.goms.gomsbook.ai.accessibility.validation.AccessibilityValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.DefaultAccessibilityValidator;
import kr.co.goms.gomsbook.ai.agent.AgentExecutor;
import kr.co.goms.gomsbook.ai.agent.DefaultAgentExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalHandlerRegistry;
import kr.co.goms.gomsbook.ai.agent.approval.AgentApprovalService;
import kr.co.goms.gomsbook.ai.agent.approval.DefaultAgentApprovalExecutor;
import kr.co.goms.gomsbook.ai.agent.approval.DefaultAgentApprovalHandlerRegistry;
import kr.co.goms.gomsbook.ai.agent.approval.DefaultAgentApprovalService;
import kr.co.goms.gomsbook.ai.agent.approval.handler.ApplyEpubStylesheetApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.ApplyEpubTemplateApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateBasicXhtmlApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubProjectApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.DeleteEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.DeleteEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubCopyrightApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubManifestApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubMetadataApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubNavigationApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubPartApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubSpineApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubXhtmlAttributeApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubCopyrightApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubNavigationApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubPartApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventPublisher;
import kr.co.goms.gomsbook.ai.agent.event.DefaultAgentEventPublisher;
import kr.co.goms.gomsbook.ai.api.agent.sse.AgentSseEventDispatcher;
import kr.co.goms.gomsbook.ai.api.agent.sse.SseAgentEventListener;
import kr.co.goms.gomsbook.ai.conversation.repository.AiConversationMessageRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.AiConversationRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.jdbc.JdbcAiConversationMessageRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.jdbc.JdbcAiConversationRepository;
import kr.co.goms.gomsbook.ai.conversation.service.ConversationService;
import kr.co.goms.gomsbook.ai.epub.plan.project.CreateEpubProjectPlanService;
import kr.co.goms.gomsbook.ai.epub.plan.project.CreateEpubProjectPlanStore;
import kr.co.goms.gomsbook.ai.epub.plan.project.DefaultCreateEpubProjectPlanService;
import kr.co.goms.gomsbook.ai.epub.plan.project.InMemoryCreateEpubProjectPlanStore;
import kr.co.goms.gomsbook.ai.epub.service.EpubCheckRunner;
import kr.co.goms.gomsbook.ai.epub.service.EpubStructureValidator;
import kr.co.goms.gomsbook.ai.epub.service.LatestPublishedEpubResolver;
import kr.co.goms.gomsbook.ai.epub.service.PublishDirectoryProvider;
import kr.co.goms.gomsbook.ai.epub.updater.navigation.DefaultEpubNavigationUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.navigation.EpubNavigationUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.pkg.DefaultEpubPackageUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.pkg.EpubPackageUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.DefaultEpubXhtmlUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.EpubXhtmlUpdater;
import kr.co.goms.gomsbook.ai.epub.validation.DefaultEpubProjectAccessibilityValidator;
import kr.co.goms.gomsbook.ai.epub.validation.DefaultEpubProjectValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckRunnerValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubProjectAccessibilityValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubProjectValidator;
import kr.co.goms.gomsbook.ai.json.GsonJsonMapper;
import kr.co.goms.gomsbook.ai.json.JsonMapper;
import kr.co.goms.gomsbook.ai.llm.LlmClient;
import kr.co.goms.gomsbook.ai.llm.model.ChatModelProvider;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaConfiguration;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaLlmClient;
import kr.co.goms.gomsbook.ai.logging.ExecutionLogger;
import kr.co.goms.gomsbook.ai.project.CurrentProjectProvider;
import kr.co.goms.gomsbook.ai.project.CurrentProjectStore;
import kr.co.goms.gomsbook.ai.project.DefaultCurrentProjectProvider;
import kr.co.goms.gomsbook.ai.project.InMemoryCurrentProjectStore;
import kr.co.goms.gomsbook.ai.tool.AgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultAgentToolRegistrar;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.DefaultToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.DefaultToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionMapper;
import kr.co.goms.gomsbook.ai.tool.ToolDefinitionProvider;
import kr.co.goms.gomsbook.ai.tool.ToolExecutor;
import kr.co.goms.gomsbook.ai.tool.ToolRegistry;
import kr.co.goms.gomsbook.ai.tool.epub.author.CreateEpubAuthorTool;
import kr.co.goms.gomsbook.ai.tool.epub.author.DeleteEpubAuthorTool;
import kr.co.goms.gomsbook.ai.tool.epub.author.ReadEpubAuthorTool;
import kr.co.goms.gomsbook.ai.tool.epub.author.UpdateEpubAuthorTool;
import kr.co.goms.gomsbook.ai.tool.epub.chapter.CreateEpubChapterTool;
import kr.co.goms.gomsbook.ai.tool.epub.chapter.DeleteEpubChapterTool;
import kr.co.goms.gomsbook.ai.tool.epub.chapter.UpdateEpubChapterTool;
import kr.co.goms.gomsbook.ai.tool.epub.copyright.CreateEpubCopyrightTool;
import kr.co.goms.gomsbook.ai.tool.epub.copyright.UpdateEpubCopyrightTool;
import kr.co.goms.gomsbook.ai.tool.epub.generation.chapter.CreateBasicXhtmlTool;
import kr.co.goms.gomsbook.ai.tool.epub.manifest.UpdateEpubManifestTool;
import kr.co.goms.gomsbook.ai.tool.epub.metadata.UpdateEpubMetadataTool;
import kr.co.goms.gomsbook.ai.tool.epub.navigation.CreateEpubNavigationTool;
import kr.co.goms.gomsbook.ai.tool.epub.navigation.UpdateEpubNavigationTool;
import kr.co.goms.gomsbook.ai.tool.epub.part.CreateEpubPartTool;
import kr.co.goms.gomsbook.ai.tool.epub.part.UpdateEpubPartTool;
import kr.co.goms.gomsbook.ai.tool.epub.project.ApplyEpubTemplateTool;
import kr.co.goms.gomsbook.ai.tool.epub.project.CreateEpubProjectTool;
import kr.co.goms.gomsbook.ai.tool.epub.resource.ApplyEpubStylesheetTool;
import kr.co.goms.gomsbook.ai.tool.epub.spine.UpdateEpubSpineTool;
import kr.co.goms.gomsbook.ai.tool.epub.validation.ValidateEpubProjectTool;
import kr.co.goms.gomsbook.ai.tool.epub.xhtml.UpdateEpubXhtmlAttributeTool;
import kr.co.goms.gomsbook.ai.epub.generation.author.DefaultEpubAuthorXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.author.EpubAuthorService;
import kr.co.goms.gomsbook.ai.epub.generation.author.EpubAuthorXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.DefaultEpubChapterXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.EpubChapterService;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.EpubChapterXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.DefaultEpubNavigationXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.EpubNavigationService;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.EpubNavigationXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.part.EpubPartService;
import kr.co.goms.gomsbook.ai.epub.policy.spine.DefaultEpubSpineOrderPolicy;
import kr.co.goms.gomsbook.ai.epub.policy.spine.EpubSpineOrderPolicy;
import kr.co.goms.gomsbook.ai.epub.resource.stylesheet.EpubStylesheetResolver;

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

        return OllamaConfiguration.builder()
                .baseUrl(
                        ollamaBaseUrl)
                .model(
                        chatModel)
                .chatModel(
                        chatModel)
                .embeddingModel(
                        embeddingModel)
                .build();
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
    public ToolExecutor toolExecutor(ToolRegistry toolRegistry, ExecutionLogger executionLogger) {

        return new DefaultToolExecutor(toolRegistry, executionLogger);
    }


    /*
     * ============================================================
     * Agent
     * ============================================================
     */

    @Bean
    public AgentExecutor agentExecutor(LlmClient llmClient, ToolExecutor toolExecutor, ToolDefinitionProvider toolDefinitionProvider, ChatModelProvider chatModelProvider) {

        return new DefaultAgentExecutor(llmClient, toolExecutor, toolDefinitionProvider, chatModelProvider);
    }


    /*
     * ============================================================
     * Agent Event
     * ============================================================
     */

    @Bean
    public DefaultAgentEventPublisher agentEventPublisher(AgentSseEventDispatcher dispatcher) {

        DefaultAgentEventPublisher publisher = new DefaultAgentEventPublisher();

        publisher.addListener(new SseAgentEventListener(dispatcher));

        return publisher;
    }
    

    @Bean
    public JsonMapper jsonMapper() {

        return new GsonJsonMapper();
    }

    @Bean
    public Gson gson() {

        return new Gson();
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
    public CurrentProjectStore currentProjectStore() {
        return new InMemoryCurrentProjectStore();
    }

    @Bean
    public CurrentProjectProvider currentProjectProvider(CurrentProjectStore currentProjectStore) {

        return new DefaultCurrentProjectProvider(currentProjectStore);
    }
    

    @Bean
    public PublishDirectoryProvider publishDirectoryProvider() {

        return () -> Path.of(
                publishDirectory);
    }

    @Bean
    public CreateEpubProjectPlanStore createEpubProjectPlanStore() {

        return new InMemoryCreateEpubProjectPlanStore();
    }


    @Bean
    public CreateEpubProjectPlanService createEpubProjectPlanService(CreateEpubProjectPlanStore store) {

        return new DefaultCreateEpubProjectPlanService(store);
    }

    /*
     * ============================================================
     * EPUB Policy
     * ============================================================
     */

    @Bean
    public EpubSpineOrderPolicy epubSpineOrderPolicy() {
        return new DefaultEpubSpineOrderPolicy();
    }

    @Bean
    public EpubStructureValidator epubStructureValidator(EpubSpineOrderPolicy spineOrderPolicy) {
        return new EpubStructureValidator(spineOrderPolicy);
    }

    @Bean
    public LatestPublishedEpubResolver latestPublishedEpubResolver() {
        return new LatestPublishedEpubResolver();
    }

    /*
     * ============================================================
     * EPUB Author
     * ============================================================
     */

    @Bean
    public EpubAuthorXhtmlGenerator epubAuthorXhtmlGenerator() {
        return new DefaultEpubAuthorXhtmlGenerator();
    }

    @Bean
    public EpubNavigationUpdater epubNavigationUpdater() {
        return new DefaultEpubNavigationUpdater();
    }

    @Bean
    public EpubAuthorService epubAuthorService(EpubAuthorXhtmlGenerator xhtmlGenerator, EpubPackageUpdater packageUpdater, EpubNavigationUpdater navigationUpdater) {

        return new EpubAuthorService(xhtmlGenerator, packageUpdater, navigationUpdater);
    }

    @Bean
    public CreateEpubAuthorApprovalHandler createEpubAuthorApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubAuthorService epubAuthorService) {

        return new CreateEpubAuthorApprovalHandler(currentProjectProvider, epubAuthorService);
    }
    
    @Bean
    public ReadEpubAuthorTool readEpubAuthorTool(CurrentProjectProvider currentProjectProvider) {
    	
        return new ReadEpubAuthorTool(currentProjectProvider);
    }
    
    @Bean
    public UpdateEpubAuthorApprovalHandler updateEpubAuthorApprovalHandler(CurrentProjectProvider currentProjectProvider) {

        return new UpdateEpubAuthorApprovalHandler(currentProjectProvider);
    }    
    
    @Bean
    public DeleteEpubAuthorApprovalHandler deleteEpubAuthorApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubAuthorService epubAuthorService) {

        return new DeleteEpubAuthorApprovalHandler(currentProjectProvider, epubAuthorService);
    }    
    

    /*
     * ============================================================
     * EPUB Navigation
     * ============================================================
     */
    
    @Bean
    public EpubNavigationXhtmlGenerator epubNavigationXhtmlGenerator() {
        return new DefaultEpubNavigationXhtmlGenerator();
    }

    
    @Bean
    public EpubNavigationService epubNavigationService(EpubNavigationXhtmlGenerator xhtmlGenerator) {

        return new EpubNavigationService(xhtmlGenerator);
    }

    
    @Bean
    public CreateEpubNavigationApprovalHandler createEpubNavigationApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubNavigationService epubNavigationService) {

        return new CreateEpubNavigationApprovalHandler(currentProjectProvider, epubNavigationService);
    }
    
    @Bean
    public UpdateEpubNavigationApprovalHandler updateEpubNavigationApprovalHandler(
            CurrentProjectProvider currentProjectProvider,
            EpubNavigationUpdater navigationUpdater,
            Gson gson) {

        return new UpdateEpubNavigationApprovalHandler(
                currentProjectProvider,
                navigationUpdater,
                gson);
    }
    
    /*
     * ============================================================
     * EPUB Part
     * ============================================================
     */
    

    @Bean
    public EpubPartService epubPartService(EpubPackageUpdater packageUpdater, EpubNavigationUpdater navigationUpdater) {

        return new EpubPartService(packageUpdater, navigationUpdater);
    }
    
    @Bean
    public CreateEpubPartApprovalHandler createEpubPartApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubPartService epubPartService) {

        return new CreateEpubPartApprovalHandler(currentProjectProvider, epubPartService);
    }
    
    @Bean
    public UpdateEpubPartApprovalHandler updateEpubPartApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubPartService epubPartService) {

        return new UpdateEpubPartApprovalHandler(currentProjectProvider, epubPartService);
    }    

    
    /*
     * ============================================================
     * EPUB Chapter
     * ============================================================
     */
    
    @Bean
    public EpubChapterXhtmlGenerator epubChapterXhtmlGenerator() {
        return new DefaultEpubChapterXhtmlGenerator();
    }

    @Bean
    public EpubStylesheetResolver epubStylesheetResolver() {
        return new EpubStylesheetResolver();
    }

    @Bean
    public EpubChapterService epubChapterService(EpubChapterXhtmlGenerator xhtmlGenerator, EpubStylesheetResolver stylesheetResolver, EpubPackageUpdater packageUpdater, EpubNavigationUpdater navigationUpdater) {
        return new EpubChapterService(xhtmlGenerator, stylesheetResolver, packageUpdater, navigationUpdater);
    }

    @Bean
    public CreateEpubChapterApprovalHandler createEpubChapterApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubChapterService epubChapterService) {
        return new CreateEpubChapterApprovalHandler(currentProjectProvider, epubChapterService);
    }

    @Bean
    public UpdateEpubChapterApprovalHandler updateEpubChapterApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubChapterService epubChapterService) {
        return new UpdateEpubChapterApprovalHandler(currentProjectProvider, epubChapterService);
    }

    @Bean
    public DeleteEpubChapterApprovalHandler deleteEpubChapterApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubChapterService epubChapterService) {
        return new DeleteEpubChapterApprovalHandler(currentProjectProvider, epubChapterService);
    }

    /*
     * ============================================================
     * EPUB Package
     * ============================================================
     */

    @Bean
    public EpubPackageUpdater epubPackageUpdater(EpubSpineOrderPolicy spineOrderPolicy) {
        return new DefaultEpubPackageUpdater(spineOrderPolicy);
    }
    
    @Bean
    public UpdateEpubSpineApprovalHandler updateEpubSpineApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubPackageUpdater packageUpdater) {
        return new UpdateEpubSpineApprovalHandler(currentProjectProvider, packageUpdater);
    }
    
    @Bean
    public UpdateEpubManifestApprovalHandler updateEpubManifestApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubPackageUpdater packageUpdater) {
        return new UpdateEpubManifestApprovalHandler(currentProjectProvider, packageUpdater);
    }
    
    @Bean
    public UpdateEpubMetadataApprovalHandler updateEpubMetadataApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubPackageUpdater packageUpdater, Gson gson) {
        return new UpdateEpubMetadataApprovalHandler(currentProjectProvider, packageUpdater, gson);
    }
    
    /*
     * ============================================================
     * Agent Approval
     * ============================================================
     */

    @Bean
    public AgentApprovalService agentApprovalService() {

        return new DefaultAgentApprovalService();
    }


    @Bean
    public CreateBasicXhtmlApprovalHandler createBasicXhtmlApprovalHandler(CurrentProjectProvider currentProjectProvider) {

        return new CreateBasicXhtmlApprovalHandler(currentProjectProvider);
    }


    @Bean
    public CreateEpubProjectApprovalHandler createEpubProjectApprovalHandler(CreateEpubProjectPlanService createEpubProjectPlanService, ToolExecutor toolExecutor) {

        return new CreateEpubProjectApprovalHandler(createEpubProjectPlanService, toolExecutor);
    }

    @Bean
    public ApplyEpubTemplateApprovalHandler applyEpubTemplateApprovalHandler(ToolExecutor toolExecutor) {

        return new ApplyEpubTemplateApprovalHandler(toolExecutor);
    }
    
    @Bean
    public UpdateEpubCopyrightApprovalHandler updateEpubCopyrightApprovalHandler(CurrentProjectProvider currentProjectProvider) {

        return new UpdateEpubCopyrightApprovalHandler(currentProjectProvider);
    }    
    
    @Bean
    public CreateEpubCopyrightApprovalHandler createEpubCopyrightApprovalHandler(CurrentProjectProvider currentProjectProvider) {

        return new CreateEpubCopyrightApprovalHandler(currentProjectProvider);
    }
    
    /*
     * ============================================================
     * EPUB XHTML
     * ============================================================
     */

    @Bean
    public EpubXhtmlUpdater epubXhtmlUpdater() {

        return new DefaultEpubXhtmlUpdater();
    }    
    
    @Bean
    public UpdateEpubXhtmlAttributeApprovalHandler updateEpubXhtmlAttributeApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubXhtmlUpdater epubXhtmlUpdater) {

        return new UpdateEpubXhtmlAttributeApprovalHandler(currentProjectProvider, epubXhtmlUpdater);
    }
    
    
    /*
     * ============================================================
     * Conversation
     * ============================================================
     */

    @Bean
    public AiConversationRepository aiConversationRepository(JdbcTemplate jdbcTemplate) {

        return new JdbcAiConversationRepository(jdbcTemplate);
    }

    @Bean
    public AiConversationMessageRepository aiConversationMessageRepository(JdbcTemplate jdbcTemplate) {

        return new JdbcAiConversationMessageRepository(jdbcTemplate);
    }

    @Bean
    public ConversationService conversationService(AiConversationRepository conversationRepository, AiConversationMessageRepository messageRepository) {

        return new ConversationService(conversationRepository, messageRepository);
    }
    
    /*
     * ============================================================
     * EPUB Resource
     * ============================================================
     */
    
    @Bean
    public ApplyEpubStylesheetApprovalHandler applyEpubStylesheetApprovalHandler(CurrentProjectProvider currentProjectProvider) {

        return new ApplyEpubStylesheetApprovalHandler(currentProjectProvider);
    }
       
    
    /*
     * ============================================================
     * EPUB Project Plan
     * ============================================================
     */
    
    @Bean
    public EpubCheckRunner epubCheckRunner() {

        return new EpubCheckRunner(
                Path.of(
                        epubCheckDirectory),
                epubCheckVersion);
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
    public EpubProjectAccessibilityValidator epubProjectAccessibilityValidator() {

        return new DefaultEpubProjectAccessibilityValidator(accessibilityValidator());
    }

    @Bean
    public EpubProjectValidator epubProjectValidator() {

        return new DefaultEpubProjectValidator();
    }

    @Bean
    public ValidateEpubProjectTool validateEpubProjectTool(CurrentProjectProvider currentProjectProvider, EpubProjectValidator epubProjectValidator) {
        return new ValidateEpubProjectTool(currentProjectProvider, epubProjectValidator);
    }
    
    
    @Bean
    public AgentApprovalHandlerRegistry agentApprovalHandlerRegistry(
            CreateBasicXhtmlApprovalHandler createBasicXhtmlApprovalHandler,
            CreateEpubProjectApprovalHandler createEpubProjectApprovalHandler,
            ApplyEpubTemplateApprovalHandler applyEpubTemplateApprovalHandler,
            ApplyEpubStylesheetApprovalHandler applyEpubStylesheetApprovalHandler,            
            UpdateEpubCopyrightApprovalHandler updateEpubCopyrightApprovalHandler,
            CreateEpubCopyrightApprovalHandler createEpubCopyrightApprovalHandler,
            CreateEpubAuthorApprovalHandler createEpubAuthorApprovalHandler,
            UpdateEpubAuthorApprovalHandler updateEpubAuthorApprovalHandler,
            DeleteEpubAuthorApprovalHandler deleteEpubAuthorApprovalHandler,
            CreateEpubNavigationApprovalHandler createEpubNavigationApprovalHandler,
            UpdateEpubNavigationApprovalHandler updateEpubNavigationApprovalHandler,
            CreateEpubPartApprovalHandler createEpubPartApprovalHandler,
            UpdateEpubPartApprovalHandler updateEpubPartApprovalHandler,
            CreateEpubChapterApprovalHandler createEpubChapterApprovalHandler,
            UpdateEpubChapterApprovalHandler updateEpubChapterApprovalHandler,
            DeleteEpubChapterApprovalHandler deleteEpubChapterApprovalHandler,
            UpdateEpubSpineApprovalHandler updateEpubSpineApprovalHandler,
            UpdateEpubManifestApprovalHandler updateEpubManifestApprovalHandler,
            UpdateEpubMetadataApprovalHandler updateEpubMetadataApprovalHandler,
            UpdateEpubXhtmlAttributeApprovalHandler updateEpubXhtmlAttributeApprovalHandler 
            ) {

        AgentApprovalHandlerRegistry registry = new DefaultAgentApprovalHandlerRegistry();

        registry.register(CreateBasicXhtmlTool.TOOL_NAME, createBasicXhtmlApprovalHandler);
        registry.register(CreateEpubProjectTool.TOOL_NAME, createEpubProjectApprovalHandler);
        registry.register(ApplyEpubTemplateTool.TOOL_NAME, applyEpubTemplateApprovalHandler);
        registry.register(ApplyEpubStylesheetTool.TOOL_NAME, applyEpubStylesheetApprovalHandler);
        registry.register(UpdateEpubCopyrightTool.TOOL_NAME, updateEpubCopyrightApprovalHandler);
        registry.register(CreateEpubCopyrightTool.TOOL_NAME, createEpubCopyrightApprovalHandler);
        registry.register(CreateEpubAuthorTool.TOOL_NAME, createEpubAuthorApprovalHandler);
        registry.register(UpdateEpubAuthorTool.TOOL_NAME, updateEpubAuthorApprovalHandler);
        registry.register(DeleteEpubAuthorTool.TOOL_NAME, deleteEpubAuthorApprovalHandler);
        registry.register(CreateEpubNavigationTool.TOOL_NAME, createEpubNavigationApprovalHandler);
        registry.register(UpdateEpubNavigationTool.TOOL_NAME, updateEpubNavigationApprovalHandler);
        registry.register(CreateEpubPartTool.TOOL_NAME, createEpubPartApprovalHandler);
        registry.register(UpdateEpubPartTool.TOOL_NAME, updateEpubPartApprovalHandler);
        registry.register(CreateEpubChapterTool.TOOL_NAME, createEpubChapterApprovalHandler);
        registry.register(UpdateEpubChapterTool.TOOL_NAME, updateEpubChapterApprovalHandler);
        registry.register(DeleteEpubChapterTool.TOOL_NAME, deleteEpubChapterApprovalHandler);
        registry.register(UpdateEpubSpineTool.TOOL_NAME, updateEpubSpineApprovalHandler);
        registry.register(UpdateEpubManifestTool.TOOL_NAME, updateEpubManifestApprovalHandler);
        registry.register(UpdateEpubMetadataTool.TOOL_NAME, updateEpubMetadataApprovalHandler);
        registry.register(UpdateEpubXhtmlAttributeTool.TOOL_NAME, updateEpubXhtmlAttributeApprovalHandler);

        return registry;
    }


    @Bean
    public AgentApprovalExecutor agentApprovalExecutor(AgentApprovalHandlerRegistry handlerRegistry) {

        return new DefaultAgentApprovalExecutor(handlerRegistry);
    }


    /*
     * ============================================================
     * Agent Tool
     * ============================================================
     */

    @Bean
    public AgentToolRegistrar agentToolRegistrar(
            CurrentProjectProvider currentProjectProvider,
            PublishDirectoryProvider publishDirectoryProvider,
            EpubCheckValidator epubCheckValidator,
            AccessibilityValidator accessibilityValidator,
            AgentApprovalService approvalService,
            AgentEventPublisher eventPublisher,
            CurrentProjectStore currentProjectStore,
            CreateEpubProjectPlanService createEpubProjectPlanService,
            LatestPublishedEpubResolver latestPublishedEpubResolver, EpubStructureValidator epubStructureValidator,
            Gson gson,
			EpubProjectAccessibilityValidator epubProjectAccessibilityValidator, EpubProjectValidator epubProjectValidator
    		) {

        Path epubProjectsRoot = Path.of("C:\\1004.GomsBook\\03.Project");

        return new DefaultAgentToolRegistrar(
                currentProjectProvider,
                publishDirectoryProvider,
                epubCheckValidator,
                accessibilityValidator,
                approvalService,
                eventPublisher,
                currentProjectStore,
                createEpubProjectPlanService,
                epubProjectsRoot,
                latestPublishedEpubResolver,
                epubStructureValidator,
                gson,
                epubProjectAccessibilityValidator,
                epubProjectValidator
        		);
    }


    @Bean
    public ToolRegistry toolRegistry(AgentToolRegistrar agentToolRegistrar) {

        ToolRegistry registry = new ToolRegistry();

        agentToolRegistrar.registerTools(registry);

        System.out.println("[GomsBook AI API] Registered tools = " + registry.getToolNames());

        return registry;
    }


   
}