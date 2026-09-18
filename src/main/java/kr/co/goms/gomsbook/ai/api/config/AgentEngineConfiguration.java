/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.gomsbook.ai.api.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.gson.Gson;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import kr.co.goms.gomsbook.ai.accessibility.validation.AccessibilityValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.DefaultAccessibilityValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.DefaultEpubProjectAccessibilityValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.DefaultEpubProjectValidator;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.AriaAccessibilityRule;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.DocumentLanguageAccessibilityRule;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.HeadingAccessibilityRule;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.ImageAltAccessibilityRule;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.LinkAccessibilityRule;
import kr.co.goms.gomsbook.ai.accessibility.validation.rule.TableAccessibilityRule;
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
import kr.co.goms.gomsbook.ai.agent.approval.handler.CleanEpubTypographyApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CleanEpubXhtmlApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateBasicXhtmlApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubCopyrightApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubLoiApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubLotApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubNavigationApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubPartApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.CreateEpubProjectApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.DeleteEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.DeleteEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.DeleteRagProjectIndexApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.FixEpubKoreanTypoApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.ReleaseEpubApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubAuthorApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubChapterApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubCopyrightApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubManifestApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubMetadataApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubNavigationApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubPartApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubSpineApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.approval.handler.UpdateEpubXhtmlAttributeApprovalHandler;
import kr.co.goms.gomsbook.ai.agent.event.AgentEventPublisher;
import kr.co.goms.gomsbook.ai.agent.event.DefaultAgentEventPublisher;
import kr.co.goms.gomsbook.ai.agent.prompt.ToolResponsePromptResolver;
import kr.co.goms.gomsbook.ai.api.agent.prompt.ToolResponsePromptRegistry;
import kr.co.goms.gomsbook.ai.api.agent.prompt.rule.AgentPromptRule;
import kr.co.goms.gomsbook.ai.api.agent.prompt.rule.ValidateEpubFilePromptRule;
import kr.co.goms.gomsbook.ai.api.agent.sse.AgentSseEventDispatcher;
import kr.co.goms.gomsbook.ai.api.agent.sse.SseAgentEventListener;
import kr.co.goms.gomsbook.ai.conversation.repository.AiConversationMessageRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.AiConversationRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.jdbc.JdbcAiConversationMessageRepository;
import kr.co.goms.gomsbook.ai.conversation.repository.jdbc.JdbcAiConversationRepository;
import kr.co.goms.gomsbook.ai.conversation.service.ConversationService;
import kr.co.goms.gomsbook.ai.epub.generation.author.DefaultEpubAuthorXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.author.EpubAuthorService;
import kr.co.goms.gomsbook.ai.epub.generation.author.EpubAuthorXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.DefaultEpubChapterXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.EpubChapterService;
import kr.co.goms.gomsbook.ai.epub.generation.chapter.EpubChapterXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.loi.DefaultEpubLoiGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.loi.EpubLoiGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.lot.DefaultEpubLotGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.lot.EpubLotGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.DefaultEpubNavigationXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.EpubNavigationService;
import kr.co.goms.gomsbook.ai.epub.generation.navigation.EpubNavigationXhtmlGenerator;
import kr.co.goms.gomsbook.ai.epub.generation.part.EpubPartService;
import kr.co.goms.gomsbook.ai.epub.plan.project.CreateEpubProjectPlanService;
import kr.co.goms.gomsbook.ai.epub.plan.project.CreateEpubProjectPlanStore;
import kr.co.goms.gomsbook.ai.epub.plan.project.DefaultCreateEpubProjectPlanService;
import kr.co.goms.gomsbook.ai.epub.plan.project.InMemoryCreateEpubProjectPlanStore;
import kr.co.goms.gomsbook.ai.epub.policy.spine.DefaultEpubSpineOrderPolicy;
import kr.co.goms.gomsbook.ai.epub.policy.spine.EpubSpineOrderPolicy;
import kr.co.goms.gomsbook.ai.epub.proofreading.DictionaryKoreanTypoChecker;
import kr.co.goms.gomsbook.ai.epub.proofreading.KoreanTypoChecker;
import kr.co.goms.gomsbook.ai.epub.publish.DefaultEpubArtifactFingerprintService;
import kr.co.goms.gomsbook.ai.epub.publish.EpubArtifactFingerprintService;
import kr.co.goms.gomsbook.ai.epub.reader.pkg.DefaultEpubManifestReader;
import kr.co.goms.gomsbook.ai.epub.reader.pkg.DefaultEpubSpineReader;
import kr.co.goms.gomsbook.ai.epub.reader.pkg.EpubManifestReader;
import kr.co.goms.gomsbook.ai.epub.reader.pkg.EpubSpineReader;
import kr.co.goms.gomsbook.ai.epub.release.DefaultEpubReleasePolicy;
import kr.co.goms.gomsbook.ai.epub.release.DefaultEpubReleaseService;
import kr.co.goms.gomsbook.ai.epub.release.EpubReleasePolicy;
import kr.co.goms.gomsbook.ai.epub.release.EpubReleaseRepository;
import kr.co.goms.gomsbook.ai.epub.release.EpubReleaseService;
import kr.co.goms.gomsbook.ai.epub.release.FileSystemEpubReleaseRepository;
import kr.co.goms.gomsbook.ai.epub.resource.stylesheet.EpubStylesheetResolver;
import kr.co.goms.gomsbook.ai.epub.service.DefaultEpubLoiService;
import kr.co.goms.gomsbook.ai.epub.service.DefaultEpubLotService;
import kr.co.goms.gomsbook.ai.epub.service.DefaultEpubXhtmlCleanupService;
import kr.co.goms.gomsbook.ai.epub.service.EpubCheckRunner;
import kr.co.goms.gomsbook.ai.epub.service.EpubLoiService;
import kr.co.goms.gomsbook.ai.epub.service.EpubLotService;
import kr.co.goms.gomsbook.ai.epub.service.EpubStructureValidator;
import kr.co.goms.gomsbook.ai.epub.service.EpubXhtmlCleanupService;
import kr.co.goms.gomsbook.ai.epub.service.LatestPublishedEpubResolver;
import kr.co.goms.gomsbook.ai.epub.service.PublishDirectoryProvider;
import kr.co.goms.gomsbook.ai.epub.updater.navigation.DefaultEpubNavigationUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.navigation.EpubNavigationUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.pkg.DefaultEpubPackageUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.pkg.EpubPackageUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.DefaultEpubTypographyUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.DefaultEpubXhtmlUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.EpubTypographyUpdater;
import kr.co.goms.gomsbook.ai.epub.updater.xhtml.EpubXhtmlUpdater;
import kr.co.goms.gomsbook.ai.epub.validation.DefaultEpubFileCheckIssueAnalyzer;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckRunnerValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubCheckValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubFileCheckIssueAnalyzer;
import kr.co.goms.gomsbook.ai.epub.validation.EpubProjectAccessibilityValidator;
import kr.co.goms.gomsbook.ai.epub.validation.EpubProjectValidator;
import kr.co.goms.gomsbook.ai.epub.validation.fix.DefaultEpubFileCheckFixPlan;
import kr.co.goms.gomsbook.ai.epub.validation.fix.DefaultEpubFileCheckFixResolver;
import kr.co.goms.gomsbook.ai.epub.validation.fix.DefaultEpubFileCheckFixService;
import kr.co.goms.gomsbook.ai.epub.validation.fix.EpubFileCheckFixPlan;
import kr.co.goms.gomsbook.ai.epub.validation.fix.EpubFileCheckFixResolver;
import kr.co.goms.gomsbook.ai.epub.validation.fix.EpubFileCheckFixService;
import kr.co.goms.gomsbook.ai.json.GsonJsonMapper;
import kr.co.goms.gomsbook.ai.json.JsonMapper;
import kr.co.goms.gomsbook.ai.llm.LlmClient;
import kr.co.goms.gomsbook.ai.llm.model.ChatModelProvider;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaConfiguration;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaEmbeddingClient;
import kr.co.goms.gomsbook.ai.llm.ollama.OllamaLlmClient;
import kr.co.goms.gomsbook.ai.logging.ExecutionLogger;
import kr.co.goms.gomsbook.ai.project.CurrentProjectProvider;
import kr.co.goms.gomsbook.ai.project.CurrentProjectStore;
import kr.co.goms.gomsbook.ai.project.DefaultCurrentProjectProvider;
import kr.co.goms.gomsbook.ai.project.InMemoryCurrentProjectStore;
import kr.co.goms.gomsbook.ai.rag.DefaultRagService;
import kr.co.goms.gomsbook.ai.rag.RagService;
import kr.co.goms.gomsbook.ai.rag.context.RagContextBuilder;
import kr.co.goms.gomsbook.ai.rag.document.DefaultDocumentLoader;
import kr.co.goms.gomsbook.ai.rag.document.DocumentLoader;
import kr.co.goms.gomsbook.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.gomsbook.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.gomsbook.ai.rag.eval.benchmark.VectorStoreBenchmarkExecutionService;
import kr.co.goms.gomsbook.ai.rag.eval.benchmark.VectorStoreBenchmarkService;
import kr.co.goms.gomsbook.ai.rag.eval.benchmark.DefaultVectorStoreBenchmarkService;
import kr.co.goms.gomsbook.ai.rag.eval.benchmark.VectorStoreBenchmarkReportWriter;
import kr.co.goms.gomsbook.ai.rag.eval.benchmark.DefaultVectorStoreBenchmarkExecutionService;
		
		

import kr.co.goms.gomsbook.ai.rag.eval.comparison.DefaultRagEvaluationComparisonService;
import kr.co.goms.gomsbook.ai.rag.eval.comparison.RagEvaluationComparisonService;
import kr.co.goms.gomsbook.ai.rag.eval.comparison.RagEvaluationComparisonWriter;
import kr.co.goms.gomsbook.ai.rag.eval.comparison.RagEvaluationReportComparator;
import kr.co.goms.gomsbook.ai.rag.eval.dataset.RagEvaluationDatasetLoader;
import kr.co.goms.gomsbook.ai.rag.eval.mapper.RagRetrievalResultMapper;
import kr.co.goms.gomsbook.ai.rag.eval.path.DefaultRagEvaluationPathResolver;
import kr.co.goms.gomsbook.ai.rag.eval.path.RagEvaluationPathResolver;
import kr.co.goms.gomsbook.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.gomsbook.ai.rag.eval.profile.RagEvaluationVersion;
import kr.co.goms.gomsbook.ai.rag.eval.retrieval.DefaultRagRetrievalEvaluator;
import kr.co.goms.gomsbook.ai.rag.eval.retrieval.RagRetrievalEvaluator;
import kr.co.goms.gomsbook.ai.rag.eval.runner.RagRetrievalEvaluationRunner;
import kr.co.goms.gomsbook.ai.rag.eval.runtime.RagEvaluationComponentFactory;
import kr.co.goms.gomsbook.ai.rag.eval.runtime.RagEvaluationRuntime;
import kr.co.goms.gomsbook.ai.rag.eval.service.DefaultRagEvaluationService;
import kr.co.goms.gomsbook.ai.rag.eval.service.DefaultRagRetrievalEvaluationService;
import kr.co.goms.gomsbook.ai.rag.eval.service.RagEvaluationService;
import kr.co.goms.gomsbook.ai.rag.eval.service.RagRetrievalEvaluationService;
import kr.co.goms.gomsbook.ai.rag.expansion.ChunkContextProvider;
import kr.co.goms.gomsbook.ai.rag.expansion.ContextExpander;
import kr.co.goms.gomsbook.ai.rag.expansion.DefaultContextExpander;
import kr.co.goms.gomsbook.ai.rag.expansion.InMemoryChunkContextProvider;
import kr.co.goms.gomsbook.ai.rag.graph.GraphExpansionProvider;
import kr.co.goms.gomsbook.ai.rag.graph.epub.DefaultEpubGraphDocumentPolicy;
import kr.co.goms.gomsbook.ai.rag.graph.epub.DefaultEpubGraphExpansionProvider;
import kr.co.goms.gomsbook.ai.rag.graph.epub.EpubGraphDocumentPolicy;
import kr.co.goms.gomsbook.ai.rag.hash.HashService;
import kr.co.goms.gomsbook.ai.rag.hash.Sha256HashService;
import kr.co.goms.gomsbook.ai.rag.index.DefaultDocumentIndexer;
import kr.co.goms.gomsbook.ai.rag.index.DefaultProjectRagIndexer;
import kr.co.goms.gomsbook.ai.rag.index.DefaultRagIndexer;
import kr.co.goms.gomsbook.ai.rag.index.DocumentIndexer;
import kr.co.goms.gomsbook.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.gomsbook.ai.rag.index.RagIndexRequest;
import kr.co.goms.gomsbook.ai.rag.index.RagIndexer;
import kr.co.goms.gomsbook.ai.rag.prompt.DefaultPromptAugmentor;
import kr.co.goms.gomsbook.ai.rag.prompt.PromptAugmentor;
import kr.co.goms.gomsbook.ai.rag.retrieval.DefaultRetriever;
import kr.co.goms.gomsbook.ai.rag.retrieval.HybridRetriever;
import kr.co.goms.gomsbook.ai.rag.retrieval.RagRetrievalMode;
import kr.co.goms.gomsbook.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.gomsbook.ai.rag.retrieval.Retriever;
import kr.co.goms.gomsbook.ai.rag.retrieval.VectorGraphRetriever;
import kr.co.goms.gomsbook.ai.rag.vector.InMemoryVectorStore;
import kr.co.goms.gomsbook.ai.rag.vector.VectorStore;
import kr.co.goms.gomsbook.ai.rag.vector.qdrant.QdrantConfiguration;
import kr.co.goms.gomsbook.ai.rag.vector.qdrant.QdrantVectorStore;
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
import kr.co.goms.gomsbook.ai.tool.epub.loi.CreateEpubLoiTool;
import kr.co.goms.gomsbook.ai.tool.epub.lot.CreateEpubLotTool;
import kr.co.goms.gomsbook.ai.tool.epub.manifest.UpdateEpubManifestTool;
import kr.co.goms.gomsbook.ai.tool.epub.metadata.UpdateEpubMetadataTool;
import kr.co.goms.gomsbook.ai.tool.epub.navigation.CreateEpubNavigationTool;
import kr.co.goms.gomsbook.ai.tool.epub.navigation.UpdateEpubNavigationTool;
import kr.co.goms.gomsbook.ai.tool.epub.part.CreateEpubPartTool;
import kr.co.goms.gomsbook.ai.tool.epub.part.UpdateEpubPartTool;
import kr.co.goms.gomsbook.ai.tool.epub.project.ApplyEpubTemplateTool;
import kr.co.goms.gomsbook.ai.tool.epub.project.CreateEpubProjectTool;
import kr.co.goms.gomsbook.ai.tool.epub.proofreading.FixEpubKoreanTypoTool;
import kr.co.goms.gomsbook.ai.tool.epub.release.ReleaseEpubTool;
import kr.co.goms.gomsbook.ai.tool.epub.resource.ApplyEpubStylesheetTool;
import kr.co.goms.gomsbook.ai.tool.epub.spine.UpdateEpubSpineTool;
import kr.co.goms.gomsbook.ai.tool.epub.validation.ValidateEpubProjectTool;
import kr.co.goms.gomsbook.ai.tool.epub.xhtml.CleanEpubTypographyTool;
import kr.co.goms.gomsbook.ai.tool.epub.xhtml.CleanEpubXhtmlTool;
import kr.co.goms.gomsbook.ai.tool.epub.xhtml.UpdateEpubXhtmlAttributeTool;
import kr.co.goms.gomsbook.ai.tool.rag.index.DeleteRagProjectIndexTool;

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

    @Value("${gomsbook.ai.korean-typo-dictionary}")
    private String koreanTypoDictionary;

    @Value("${gomsbook.ai.rag.evaluation.retrieval-mode:VECTOR_ONLY}")
    private String ragEvaluationRetrievalMode;

    @Value("${gomsbook.ai.rag.evaluation.version:V1}")
    private String ragEvaluationVersion;

    @Value("${gomsbook.ai.qdrant.host}")
    private String qdrantHost;

    @Value("${gomsbook.ai.qdrant.grpc-port}")
    private int qdrantGrpcPort;

    @Value("${gomsbook.ai.qdrant.collection-name}")
    private String qdrantCollectionName;

    @Value("${gomsbook.ai.qdrant.tls}")
    private boolean qdrantTls;

    /*
     * ============================================================
     * LLM / JSON
     * ============================================================
     */

    @Bean
    public OllamaConfiguration ollamaConfiguration() {

        return OllamaConfiguration.builder()
            .baseUrl(ollamaBaseUrl)
            .model(chatModel)
            .chatModel(chatModel)
            .embeddingModel(embeddingModel)
            .build();
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
    public EmbeddingClient embeddingClient(OllamaConfiguration configuration, JsonMapper jsonMapper) {

        return new OllamaEmbeddingClient(configuration, jsonMapper);
    }

    @Bean
    public ChatModelProvider chatModelProvider() {

        return () -> chatModel;
    }

    /*
     * ============================================================
     * Tool Infrastructure
     * ============================================================
     */

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
    public AgentExecutor agentExecutor(LlmClient llmClient, ToolExecutor toolExecutor, ToolDefinitionProvider toolDefinitionProvider, ChatModelProvider chatModelProvider, ToolResponsePromptResolver toolResponsePromptResolver) {

        return new DefaultAgentExecutor(
            llmClient,
            toolExecutor,
            toolDefinitionProvider,
            chatModelProvider,
            toolResponsePromptResolver
        );
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

    /*
     * ============================================================
     * Current Project
     * ============================================================
     */

    @Bean
    public CurrentProjectStore currentProjectStore() {

        return new InMemoryCurrentProjectStore();
    }

    @Bean
    public CurrentProjectProvider currentProjectProvider(CurrentProjectStore currentProjectStore) {

        return new DefaultCurrentProjectProvider(currentProjectStore);
    }

    /*
     * ============================================================
     * Common
     * ============================================================
     */

    @Bean
    public KoreanTypoChecker koreanTypoChecker() {

        return new DictionaryKoreanTypoChecker(Path.of(koreanTypoDictionary));
    }

    @Bean
    public PublishDirectoryProvider publishDirectoryProvider() {

        return () -> Path.of(publishDirectory);
    }

    @Bean
    public CreateEpubProjectPlanStore createEpubProjectPlanStore() {

        return new InMemoryCreateEpubProjectPlanStore();
    }

    @Bean
    public CreateEpubProjectPlanService createEpubProjectPlanService(CreateEpubProjectPlanStore store) {

        return new DefaultCreateEpubProjectPlanService(store);
    }

    @Bean
    public EpubArtifactFingerprintService epubArtifactFingerprintService() {

        return new DefaultEpubArtifactFingerprintService();
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
    public UpdateEpubNavigationApprovalHandler updateEpubNavigationApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubNavigationUpdater navigationUpdater, Gson gson) {

        return new UpdateEpubNavigationApprovalHandler(currentProjectProvider, navigationUpdater, gson);
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
     * EPUB XHTML / LOI / LOT
     * ============================================================
     */

    @Bean
    public EpubXhtmlUpdater epubXhtmlUpdater() {

        return new DefaultEpubXhtmlUpdater();
    }

    @Bean
    public EpubTypographyUpdater epubTypographyUpdater() {

        return new DefaultEpubTypographyUpdater();
    }

    @Bean
    public EpubXhtmlCleanupService epubXhtmlCleanupService() {

        return new DefaultEpubXhtmlCleanupService();
    }

    @Bean
    public EpubLoiGenerator epubLoiGenerator() {

        return new DefaultEpubLoiGenerator();
    }

    @Bean
    public EpubLotGenerator epubLotGenerator() {

        return new DefaultEpubLotGenerator();
    }

    @Bean
    public EpubManifestReader epubManifestReader() {

        return new DefaultEpubManifestReader();
    }

    @Bean
    public EpubSpineReader epubSpineReader() {

        return new DefaultEpubSpineReader();
    }

    @Bean
    public EpubLoiService epubLoiService(EpubLoiGenerator loiGenerator, EpubStylesheetResolver stylesheetResolver, EpubPackageUpdater packageUpdater, EpubManifestReader manifestReader, EpubSpineReader spineReader) {

        return new DefaultEpubLoiService(loiGenerator, stylesheetResolver, packageUpdater, manifestReader, spineReader);
    }

    @Bean
    public EpubLotService epubLotService(EpubLotGenerator lotGenerator, EpubStylesheetResolver stylesheetResolver, EpubPackageUpdater packageUpdater, EpubManifestReader manifestReader, EpubSpineReader spineReader) {

        return new DefaultEpubLotService(lotGenerator, stylesheetResolver, packageUpdater, manifestReader, spineReader);
    }

    @Bean
    public UpdateEpubXhtmlAttributeApprovalHandler updateEpubXhtmlAttributeApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubXhtmlUpdater epubXhtmlUpdater) {

        return new UpdateEpubXhtmlAttributeApprovalHandler(currentProjectProvider, epubXhtmlUpdater);
    }

    @Bean
    public CleanEpubTypographyApprovalHandler cleanEpubTypographyApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubTypographyUpdater epubTypographyUpdater) {

        return new CleanEpubTypographyApprovalHandler(currentProjectProvider, epubTypographyUpdater);
    }

    @Bean
    public CleanEpubXhtmlApprovalHandler cleanEpubXhtmlApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubXhtmlCleanupService epubXhtmlCleanupService, Gson gson) {

        return new CleanEpubXhtmlApprovalHandler(currentProjectProvider, epubXhtmlCleanupService, gson);
    }

    @Bean
    public CreateEpubLoiApprovalHandler createEpubLoiApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubLoiService epubLoiService, Gson gson) {

        return new CreateEpubLoiApprovalHandler(currentProjectProvider, epubLoiService, gson);
    }

    @Bean
    public CreateEpubLotApprovalHandler createEpubLotApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubLotService epubLotService, Gson gson) {

        return new CreateEpubLotApprovalHandler(currentProjectProvider, epubLotService, gson);
    }

    @Bean
    public FixEpubKoreanTypoApprovalHandler fixEpubKoreanTypoApprovalHandler(CurrentProjectProvider currentProjectProvider, Gson gson) {

        return new FixEpubKoreanTypoApprovalHandler(currentProjectProvider, gson);
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
     * EPUB Validation
     * ============================================================
     */

    @Bean
    public EpubCheckRunner epubCheckRunner() {

        return new EpubCheckRunner(Path.of(epubCheckDirectory), epubCheckVersion);
    }

    @Bean
    public EpubCheckValidator epubCheckValidator(EpubCheckRunner epubCheckRunner) {

        return new EpubCheckRunnerValidator(epubCheckRunner, epubCheckVersion);
    }

    @Bean
    public EpubFileCheckIssueAnalyzer epubFileCheckIssueAnalyzer() {

        return new DefaultEpubFileCheckIssueAnalyzer();
    }

    @Bean
    public EpubFileCheckFixPlan epubFileCheckFixPlan() {

        return new DefaultEpubFileCheckFixPlan();
    }

    @Bean
    public EpubFileCheckFixResolver epubFileCheckFixResolver() {

        return new DefaultEpubFileCheckFixResolver();
    }

    @Bean
    public AccessibilityValidator accessibilityValidator() {

        return new DefaultAccessibilityValidator(
            List.of(
                new AriaAccessibilityRule(),
                new DocumentLanguageAccessibilityRule(),
                new HeadingAccessibilityRule(),
                new ImageAltAccessibilityRule(),
                new LinkAccessibilityRule(),
                new TableAccessibilityRule()
            )
        );
    }

    @Bean
    public EpubProjectAccessibilityValidator epubProjectAccessibilityValidator(AccessibilityValidator accessibilityValidator) {

        return new DefaultEpubProjectAccessibilityValidator(accessibilityValidator);
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
    public EpubFileCheckFixService epubFileCheckFixService(EpubFileCheckIssueAnalyzer issueAnalyzer, EpubFileCheckFixPlan fixPlan, EpubFileCheckFixResolver fixResolver) {

        return new DefaultEpubFileCheckFixService(issueAnalyzer, fixPlan, fixResolver);
    }

    /*
     * ============================================================
     * Tool Response Prompt
     * ============================================================
     */

    @Bean
    public ValidateEpubFilePromptRule validateEpubFilePromptRule() {

        return new ValidateEpubFilePromptRule();
    }

    @Bean
    public ToolResponsePromptResolver toolResponsePromptResolver(List<AgentPromptRule> promptRules) {

        return new ToolResponsePromptRegistry(promptRules);
    }

    /*
     * ============================================================
     * EPUB Release
     * ============================================================
     */

    @Bean
    public EpubReleaseRepository epubReleaseRepository(PublishDirectoryProvider publishDirectoryProvider) {

        return new FileSystemEpubReleaseRepository(publishDirectoryProvider.getPublishDirectory());
    }

    @Bean
    public EpubReleasePolicy epubReleasePolicy(EpubReleaseRepository epubReleaseRepository) {

        return new DefaultEpubReleasePolicy(epubReleaseRepository);
    }

    @Bean
    public EpubReleaseService epubReleaseService(EpubReleaseRepository epubReleaseRepository, EpubReleasePolicy epubReleasePolicy) {

        return new DefaultEpubReleaseService(epubReleaseRepository, epubReleasePolicy);
    }

    @Bean
    public ReleaseEpubApprovalHandler releaseEpubApprovalHandler(CurrentProjectProvider currentProjectProvider, EpubReleaseService releaseService, Gson gson) {

        return new ReleaseEpubApprovalHandler(currentProjectProvider, releaseService, gson);
    }

    /*
     * ============================================================
     * RAG - Qdrant Infrastructure
     * ============================================================
     */

    @Bean
    public QdrantConfiguration qdrantConfiguration() {

        return new QdrantConfiguration(
            qdrantHost,
            qdrantGrpcPort,
            qdrantCollectionName,
            qdrantTls
        );
    }

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(QdrantConfiguration configuration) {

        return new QdrantClient(
            QdrantGrpcClient
                .newBuilder(
                    configuration.getHost(),
                    configuration.getGrpcPort(),
                    configuration.isTls()
                )
                .build()
        );
    }

    /*
     * ============================================================
     * RAG - VectorStore
     * ============================================================
     */

    /**
     * Golden Baseline 및 메모리 기반 비교 실험용 VectorStore입니다.
     */
    @Bean("inMemoryVectorStore")
    public VectorStore inMemoryVectorStore() {

        return new InMemoryVectorStore();
    }

    /**
     * 실제 GomsBook Runtime에서 사용하는 기본 VectorStore입니다.
     */
    @Bean("qdrantVectorStore")
    @Primary
    public VectorStore qdrantVectorStore(QdrantClient qdrantClient, QdrantConfiguration configuration) {

        return new QdrantVectorStore(qdrantClient, configuration);
    }

    /*
     * ============================================================
     * RAG - Core
     * ============================================================
     */

    @Bean
    public DocumentLoader documentLoader() {

        return new DefaultDocumentLoader();
    }

    @Bean
    public DocumentIndexer documentIndexer() {

        return new DefaultDocumentIndexer();
    }

    @Bean
    public EmbeddingModelProvider embeddingModelProvider(OllamaConfiguration configuration) {

        return () -> configuration.getEmbeddingModel();
    }

    @Bean
    public HashService hashService() {

        return new Sha256HashService();
    }

    @Bean
    public ChunkContextProvider chunkContextProvider() {

        return new InMemoryChunkContextProvider();
    }

    @Bean
    public RagIndexRequest defaultRagIndexRequest() {

        return RagIndexRequest.defaults();
    }

    @Bean
    public RagIndexer ragIndexer(
            DocumentIndexer documentIndexer,
            EmbeddingClient embeddingClient,
            EmbeddingModelProvider embeddingModelProvider,
            @Qualifier("qdrantVectorStore") VectorStore vectorStore,
            HashService hashService) {

        return new DefaultRagIndexer(
            documentIndexer,
            embeddingClient,
            embeddingModelProvider,
            vectorStore,
            hashService
        );
    }

    @Bean
    public ProjectRagIndexer projectRagIndexer(
            DocumentLoader documentLoader,
            RagIndexer ragIndexer,
            @Qualifier("qdrantVectorStore") VectorStore vectorStore,
            ChunkContextProvider chunkContextProvider,
            EmbeddingModelProvider embeddingModelProvider,
            RagIndexRequest defaultRagIndexRequest) {

        return new DefaultProjectRagIndexer(
            documentLoader,
            ragIndexer,
            vectorStore,
            chunkContextProvider,
            embeddingModelProvider,
            defaultRagIndexRequest
        );
    }

    /*
     * ============================================================
     * RAG - Retrieval
     * ============================================================
     */

    /*
    @Bean
    public org.springframework.boot.ApplicationRunner ragVectorStoreVerifier(
            @Qualifier("qdrantVectorStore") VectorStore vectorStore,
            @Qualifier("vectorOnlyRetriever") Retriever retriever) {

        return args -> {

            System.out.println("==================================================");
            System.out.println("[RAG][VERIFY] VectorStore = " + vectorStore.getClass().getName());
            System.out.println("[RAG][VERIFY] Retriever   = " + retriever.getClass().getName());
            System.out.println("==================================================");
        };
    }
    */
    
    @Bean("vectorOnlyRetriever")
    @Primary
    public Retriever vectorOnlyRetriever(EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, @Qualifier("qdrantVectorStore") VectorStore vectorStore) {

    	// reranking true, rerankWeight = 0.25
        return new DefaultRetriever(embeddingClient, embeddingModelProvider, vectorStore, RetrievalRequest.DEFAULT_TOP_K, RetrievalRequest.DEFAULT_MINIMUM_SCORE, true, 0.30, 0.58);
    }

    @Bean
    public RagContextBuilder ragContextBuilder() {

        return new RagContextBuilder();
    }

    @Bean
    public PromptAugmentor promptAugmentor() {

        return new DefaultPromptAugmentor();
    }

    @Bean
    public RagService ragService(@Qualifier("vectorOnlyRetriever") Retriever retriever, RagContextBuilder ragContextBuilder, PromptAugmentor promptAugmentor) {

        return new DefaultRagService(retriever, ragContextBuilder, promptAugmentor
        );
    }

    /*
     * ============================================================
     * RAG - Expansion / Graph / Hybrid
     * ============================================================
     */

    @Bean
    public ContextExpander contextExpander(ChunkContextProvider chunkContextProvider) {

        return new DefaultContextExpander(chunkContextProvider);
    }

    @Bean
    public EpubGraphDocumentPolicy epubGraphDocumentPolicy() {

        return new DefaultEpubGraphDocumentPolicy();
    }

    @Bean
    public GraphExpansionProvider epubGraphExpansionProvider(
            CurrentProjectProvider currentProjectProvider,
            EpubManifestReader manifestReader,
            EpubSpineReader spineReader,
            EpubGraphDocumentPolicy epubGraphDocumentPolicy) {

        return new DefaultEpubGraphExpansionProvider(
            currentProjectProvider,
            manifestReader,
            spineReader,
            epubGraphDocumentPolicy
        );
    }

    @Bean("vectorGraphRetriever")
    public Retriever vectorGraphRetriever(
            @Qualifier("vectorOnlyRetriever") Retriever vectorRetriever,
            GraphExpansionProvider graphExpansionProvider,
            RagEvaluationProfile ragEvaluationProfile) {

        return new VectorGraphRetriever(
            vectorRetriever,
            graphExpansionProvider,
            ragEvaluationProfile.getGraphWeight(),
            ragEvaluationProfile.getGraphSeedLimit(),
            ragEvaluationProfile.isGraphCandidateChunkFilterEnabled()
        );
    }

    @Bean("hybridRetriever")
    public Retriever hybridRetriever(
            @Qualifier("vectorOnlyRetriever") Retriever vectorOnlyRetriever,
            @Qualifier("vectorGraphRetriever") Retriever vectorGraphRetriever,
            RagEvaluationProfile ragEvaluationProfile) {

        return new HybridRetriever(
            vectorOnlyRetriever,
            vectorGraphRetriever,
            ragEvaluationProfile.getHybridVectorWeight(),
            ragEvaluationProfile.getHybridVectorGraphWeight(),
            ragEvaluationProfile.getHybridRrfK(),
            ragEvaluationProfile.getHybridBranchCandidateMultiplier()
        );
    }

    /*
     * ============================================================
     * RAG - Evaluation
     * ============================================================
     */

    @Bean
    public RagEvaluationProfile ragEvaluationProfile() {

        return RagEvaluationProfile.of(RagRetrievalMode.from(ragEvaluationRetrievalMode), RagEvaluationVersion.from(ragEvaluationVersion));
    }

    @Bean("ragEvaluationRetriever")
    public Retriever ragEvaluationRetriever(
            @Qualifier("vectorOnlyRetriever") Retriever vectorOnlyRetriever,
            @Qualifier("vectorGraphRetriever") Retriever vectorGraphRetriever,
            @Qualifier("hybridRetriever") Retriever hybridRetriever,
            RagEvaluationProfile ragEvaluationProfile) {

        return switch (ragEvaluationProfile.getRetrievalMode()) {
            case VECTOR_ONLY -> vectorOnlyRetriever;
            case VECTOR_GRAPH -> vectorGraphRetriever;
            case HYBRID -> hybridRetriever;
        };
    }

    @Bean
    public RagEvaluationPathResolver ragEvaluationPathResolver(PublishDirectoryProvider publishDirectoryProvider) {

        return new DefaultRagEvaluationPathResolver(publishDirectoryProvider);
    }

    @Bean
    public RagEvaluationRuntime ragEvaluationRuntime(
            CurrentProjectProvider currentProjectProvider,
            ProjectRagIndexer projectRagIndexer,
            @Qualifier("ragEvaluationRetriever") Retriever retriever,
            ContextExpander contextExpander,
            LlmClient llmClient) {

        return RagEvaluationComponentFactory.createRuntime(
            currentProjectProvider,
            projectRagIndexer,
            retriever,
            contextExpander,
            llmClient,
            chatModel
        );
    }

    @Bean
    public RagEvaluationService ragEvaluationService(
            CurrentProjectProvider currentProjectProvider,
            RagEvaluationPathResolver pathResolver,
            RagEvaluationRuntime runtime,
            RagEvaluationProfile ragEvaluationProfile) {

        return new DefaultRagEvaluationService(
            currentProjectProvider,
            pathResolver,
            runtime,
            ragEvaluationProfile
        );
    }

    @Bean
    public RagEvaluationDatasetLoader ragEvaluationDatasetLoader() {

        return new RagEvaluationDatasetLoader();
    }

    @Bean
    public RagRetrievalResultMapper ragRetrievalResultMapper() {

        return new RagRetrievalResultMapper();
    }

    @Bean
    public RagRetrievalEvaluator ragRetrievalEvaluator() {

        return new DefaultRagRetrievalEvaluator();
    }

    @Bean
    public RagRetrievalEvaluationRunner ragRetrievalEvaluationRunner(
            CurrentProjectProvider currentProjectProvider,
            ProjectRagIndexer projectRagIndexer,
            @Qualifier("ragEvaluationRetriever") Retriever retriever,
            RagRetrievalResultMapper ragRetrievalResultMapper,
            RagRetrievalEvaluator ragRetrievalEvaluator) {

        return new RagRetrievalEvaluationRunner(
            currentProjectProvider,
            projectRagIndexer,
            retriever,
            ragRetrievalResultMapper,
            ragRetrievalEvaluator
        );
    }

    @Bean
    public RagRetrievalEvaluationService ragRetrievalEvaluationService(
            CurrentProjectProvider currentProjectProvider,
            RagEvaluationPathResolver ragEvaluationPathResolver,
            RagEvaluationDatasetLoader ragEvaluationDatasetLoader,
            RagRetrievalEvaluationRunner ragRetrievalEvaluationRunner) {

        return new DefaultRagRetrievalEvaluationService(
            currentProjectProvider,
            ragEvaluationPathResolver,
            ragEvaluationDatasetLoader,
            ragRetrievalEvaluationRunner
        );
    }

    @Bean
    public RagEvaluationReportComparator ragEvaluationReportComparator() {

        return new RagEvaluationReportComparator();
    }

    @Bean
    public RagEvaluationComparisonWriter ragEvaluationComparisonWriter() {

        return new RagEvaluationComparisonWriter();
    }

    @Bean
    public RagEvaluationComparisonService ragEvaluationComparisonService(
            CurrentProjectProvider currentProjectProvider,
            RagEvaluationPathResolver ragEvaluationPathResolver,
            RagEvaluationReportComparator ragEvaluationReportComparator,
            RagEvaluationComparisonWriter ragEvaluationComparisonWriter) {

        return new DefaultRagEvaluationComparisonService(
            currentProjectProvider,
            ragEvaluationPathResolver,
            ragEvaluationReportComparator,
            ragEvaluationComparisonWriter
        );
    }
    
    @Bean
    public DeleteRagProjectIndexApprovalHandler deleteRagProjectIndexApprovalHandler(RagIndexer ragIndexer, Gson gson) {

    	return new DeleteRagProjectIndexApprovalHandler(ragIndexer, gson);
    }
    
    /*
     * ============================================================
     * RAG - BenchMark
     * ============================================================
     */
    
    @Bean
    public VectorStoreBenchmarkService vectorStoreBenchmarkService(
            RagEvaluationDatasetLoader datasetLoader,
            EmbeddingClient embeddingClient,
            EmbeddingModelProvider embeddingModelProvider,
            @Qualifier("qdrantVectorStore") VectorStore qdrantVectorStore) {

        return new DefaultVectorStoreBenchmarkService(
                datasetLoader,
                embeddingClient,
                embeddingModelProvider,
                qdrantVectorStore);
    }

    @Bean
    public VectorStoreBenchmarkReportWriter vectorStoreBenchmarkReportWriter() {
        return new VectorStoreBenchmarkReportWriter();
    }

    
    @Bean
    public VectorStoreBenchmarkExecutionService vectorStoreBenchmarkExecutionService(
            CurrentProjectProvider currentProjectProvider,
            RagEvaluationPathResolver pathResolver,
            VectorStoreBenchmarkService benchmarkService,
            VectorStoreBenchmarkReportWriter reportWriter) {

        return new DefaultVectorStoreBenchmarkExecutionService(
                currentProjectProvider,
                pathResolver,
                benchmarkService,
                reportWriter);
    }

    /*
     * ============================================================
     * AgentApprovalHandlerRegistry
     * ============================================================
     */

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
            UpdateEpubXhtmlAttributeApprovalHandler updateEpubXhtmlAttributeApprovalHandler,
            CleanEpubXhtmlApprovalHandler cleanEpubXhtmlApprovalHandler,
            CleanEpubTypographyApprovalHandler cleanEpubTypographyApprovalHandler,
            CreateEpubLoiApprovalHandler createEpubLoiApprovalHandler,
            CreateEpubLotApprovalHandler createEpubLotApprovalHandler,
            FixEpubKoreanTypoApprovalHandler fixEpubKoreanTypoApprovalHandler,
            ReleaseEpubApprovalHandler releaseEpubApprovalHandler,
            DeleteRagProjectIndexApprovalHandler deleteRagProjectIndexApprovalHandler
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
        registry.register(CleanEpubXhtmlTool.TOOL_NAME, cleanEpubXhtmlApprovalHandler);
        registry.register(CleanEpubTypographyTool.TOOL_NAME, cleanEpubTypographyApprovalHandler);
        registry.register(CreateEpubLoiTool.TOOL_NAME, createEpubLoiApprovalHandler);
        registry.register(CreateEpubLotTool.TOOL_NAME, createEpubLotApprovalHandler);
        registry.register(FixEpubKoreanTypoTool.TOOL_NAME, fixEpubKoreanTypoApprovalHandler);
        registry.register(ReleaseEpubTool.TOOL_NAME, releaseEpubApprovalHandler);
        registry.register(DeleteRagProjectIndexTool.TOOL_NAME, deleteRagProjectIndexApprovalHandler);

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
            LatestPublishedEpubResolver latestPublishedEpubResolver,
            EpubStructureValidator epubStructureValidator,
            Gson gson,
            EpubProjectAccessibilityValidator epubProjectAccessibilityValidator,
            EpubProjectValidator epubProjectValidator,
            EpubCheckRunner epubCheckRunner,
            EpubFileCheckFixService epubFileCheckFixService,
            EpubArtifactFingerprintService epubArtifactFingerprintService,
            EpubTypographyUpdater epubTypographyUpdater,
            KoreanTypoChecker koreanTypoChecker,
            EpubReleasePolicy releasePolicy,
            RagService ragService,
            ProjectRagIndexer projectRagIndexer,
            RagEvaluationService evaluationService,
            RagEvaluationProfile ragEvaluationProfile,
            RagRetrievalEvaluationService ragRetrievalEvaluationService,
            RagEvaluationComparisonService ragEvaluationComparisonService,
            VectorStoreBenchmarkExecutionService vectorStoreBenchmarkExecutionService
            ) {

        Path epubProjectsRoot = Path.of(projectRoot);

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
            epubProjectValidator,
            epubCheckRunner,
            epubFileCheckFixService,
            epubArtifactFingerprintService,
            epubTypographyUpdater,
            koreanTypoChecker,
            releasePolicy,
            ragService,
            projectRagIndexer,
            evaluationService,
            ragEvaluationProfile,
            ragRetrievalEvaluationService,
            ragEvaluationComparisonService,
            vectorStoreBenchmarkExecutionService
            
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