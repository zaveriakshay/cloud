package com.cloud.docs.ai.config;

import com.cloud.docs.ai.model.DocumentChunk;
// FIX: Import the EmbeddingService
import com.cloud.docs.ai.service.EmbeddingService;
import com.cloud.docs.ai.service.VectorStore;
import com.cloud.docs.controller.ApiRegistryService;
import com.cloud.docs.controller.DocsHubRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for the AI Assistant.
 * Responsible for initializing the VectorStore with document embeddings at application startup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AiConfig {

    private final ApiRegistryService apiRegistry;
    private final DocsHubRegistry docsHubRegistry;
    private final VectorStore vectorStore;
    // FIX: Inject the EmbeddingService to process the documents
    private final EmbeddingService embeddingService;

    /**
     * This method runs once after the application context is initialized.
     * It builds the in-memory vector store by processing all documentation content.
     */
    @PostConstruct
    public void initializeVectorStore() {
        log.info("AI Assistant: Starting vector store initialization...");
        long startTime = System.currentTimeMillis();

        vectorStore.clear(); // Ensure a clean state if re-initializing

        // 1. Ingest API Documentation
        apiRegistry.getSpecifications().forEach(apiSpec -> {
            String apiTitle = apiSpec.title();
            apiSpec.openAPI().getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (operation != null && operation.getOperationId() != null && operation.getDescription() != null) {
                        String content = operation.getDescription(); // Use description as primary content
                        String url = "/api?apiId=" + apiSpec.id() + "#" + operation.getOperationId();
                        String title = operation.getSummary();
                        String breadcrumb = apiTitle + " → " + (operation.getTags() != null && !operation.getTags().isEmpty() ? operation.getTags().get(0) : "General");

                        Map<String, String> metadata = Map.of(
                                "title", title,
                                "url", url,
                                "breadcrumb", breadcrumb,
                                "type", "api-operation",
                                "apiId", apiSpec.id(),
                                "operationId", operation.getOperationId()
                        );

                        // FIX: Uncomment the chunking and embedding logic
                        List<DocumentChunk> chunks = embeddingService.chunkDocument(content, metadata);
                        chunks.forEach(vectorStore::addDocument);
                    }
                });
            });
        });

        // 2. Ingest Markdown Documentation (Docs Hubs)
        docsHubRegistry.getAllHubs().forEach(hub -> {
            String hubTitle = hub.getArticleTree().title();
            hub.getFlatArticleList().forEach(article -> {
                if (article.content() != null && !article.content().isBlank()) {
                    String content = article.content();
                    String url = "/hub/" + hub.getHubName() + "?article=" + article.id();
                    String title = article.title();
                    String breadcrumb = hubTitle;

                    Map<String, String> metadata = Map.of(
                            "title", title,
                            "url", url,
                            "breadcrumb", breadcrumb,
                            "type", "markdown-article",
                            "hubName", hub.getHubName(),
                            "articleId", article.id()
                    );

                    // FIX: Uncomment the chunking and embedding logic
                    List<DocumentChunk> chunks = embeddingService.chunkDocument(content, metadata);
                    chunks.forEach(vectorStore::addDocument);
                }
            });
        });

        long endTime = System.currentTimeMillis();
        log.info("AI Assistant: Vector store initialized with {} document chunks in {}ms.", vectorStore.size(), (endTime - startTime));
    }

}