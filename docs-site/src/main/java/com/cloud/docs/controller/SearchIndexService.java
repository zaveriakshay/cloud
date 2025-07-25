package com.cloud.docs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private static final int MAX_RECURSION_DEPTH = 20;

    private final ApiRegistryService apiRegistry;
    private final DocsHubRegistry docsHubRegistry;
    private final SnippetService snippetService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    // REMOVED: No longer injecting a potentially ambiguous ObjectMapper.

    @Getter
    private String searchIndexJson = "[]"; // Cache the final JSON string

    @PostConstruct
    public void buildIndex() {
        log.info("Building and caching search index at application startup...");
        long startTime = System.currentTimeMillis();
        List<SearchHit> hits = new ArrayList<>();
        try {
            // 1. Add API Operations to the index
            apiRegistry.getSpecifications().forEach(apiSpec -> {
                if (apiSpec == null || apiSpec.title() == null || apiSpec.openAPI() == null) {
                    log.warn("Skipping a malformed API specification in search index generation.");
                    return;
                }
                String apiTitle = apiSpec.title();
                OpenAPI openAPI = apiSpec.openAPI();
                snippetService.getGroupedApiOperations(apiSpec.id()).forEach((tag, operations) -> {
                    if (operations == null) return;
                    operations.forEach(op -> {
                        if (op == null || op.id() == null || op.summary() == null) return;
                        snippetService.findOperationDetailsById(openAPI, op.id()).ifPresent(opDetails -> {
                            String breadcrumb = apiTitle + " → " + tag;
                            String url = "/api?apiId=" + apiSpec.id() + "#" + op.id();
                            String content = extractTextFromOperation(opDetails.operation(), openAPI);
                            hits.add(new SearchHit(op.summary(), breadcrumb, url, content));
                        });
                    });
                });
            });

            // 2. Add Docs Hub Articles to the index
            docsHubRegistry.getAllHubs().forEach(hub -> {
                if (hub == null || hub.getArticleTree() == null || hub.getArticleTree().title() == null) {
                    log.warn("Skipping a null or malformed hub in search index generation.");
                    return;
                }
                String hubTitle = hub.getArticleTree().title();
                List<DocsArticleNode> flatArticles = hub.getFlatArticleList();
                if (flatArticles == null) return;

                flatArticles.forEach(article -> {
                    if (article == null || article.id() == null || article.title() == null) {
                        log.warn("Skipping a malformed article in hub '{}'", hubTitle);
                        return;
                    }
                    String url = "/hub/" + hub.getHubName() + "?article=" + article.id();
                    String plainTextContent = renderMarkdownToPlainText(article.content());
                    hits.add(new SearchHit(article.title(), hubTitle, url, plainTextContent));
                });
            });
        } catch (Throwable t) {
            log.error("A critical error occurred while building the search index list. The index may be incomplete.", t);
        }

        // 3. Serialize the final list to a JSON string and cache it.
        try {
            // FIX: Use a dedicated, local ObjectMapper for JSON to avoid ambiguity with the YAMLMapper bean.
            ObjectMapper jsonMapper = new ObjectMapper();
            this.searchIndexJson = jsonMapper.writeValueAsString(hits);
        } catch (Exception e) {
            log.error("Failed to serialize search index to JSON string. Search will be unavailable.", e);
            this.searchIndexJson = "[]"; // Keep it as an empty JSON array to prevent client errors.
        }

        long endTime = System.currentTimeMillis();
        log.info("Search index built and cached successfully in {}ms with {} items.", (endTime - startTime), hits.size());
    }

    private String renderMarkdownToPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        String html = htmlRenderer.render(document);
        return Jsoup.parse(html).text();
    }

    private String extractTextFromOperation(Operation operation, OpenAPI openAPI) {
        if (operation == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Set<String> visitedRefs = new HashSet<>();

        if (operation.getSummary() != null) sb.append(operation.getSummary()).append(" ");
        if (operation.getDescription() != null) {
            sb.append(renderMarkdownToPlainText(operation.getDescription())).append(" ");
        }

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(p -> {
                if (p.getName() != null) sb.append(p.getName()).append(" ");
                if (p.getDescription() != null) sb.append(p.getDescription()).append(" ");
            });
        }

        if (operation.getRequestBody() != null) {
            RequestBody requestBody = operation.getRequestBody();
            if (requestBody.get$ref() != null) {
                String ref = requestBody.get$ref().substring("#/components/requestBodies/".length());
                requestBody = openAPI.getComponents().getRequestBodies().get(ref);
            }
            if (requestBody != null && requestBody.getContent() != null) {
                requestBody.getContent().values().forEach(mediaType -> {
                    if (mediaType.getSchema() != null) {
                        extractTextFromSchema(mediaType.getSchema(), openAPI, sb, visitedRefs, 0);
                    }
                });
            }
        }

        if (operation.getResponses() != null) {
            operation.getResponses().values().forEach(apiResponse -> {
                if (apiResponse.getDescription() != null) sb.append(apiResponse.getDescription()).append(" ");
                if (apiResponse.getContent() != null) {
                    apiResponse.getContent().values().forEach(mediaType -> {
                        if (mediaType.getSchema() != null) {
                            extractTextFromSchema(mediaType.getSchema(), openAPI, sb, visitedRefs, 0);
                        }
                    });
                }
            });
        }

        return sb.toString();
    }

    private void extractTextFromSchema(Schema<?> schema, OpenAPI openAPI, StringBuilder sb, Set<String> visitedRefs, int depth) {
        if (schema == null || depth > MAX_RECURSION_DEPTH) {
            return;
        }

        if (schema.get$ref() != null) {
            if (visitedRefs.contains(schema.get$ref())) {
                return;
            }
            visitedRefs.add(schema.get$ref());
            String ref = schema.get$ref().substring("#/components/schemas/".length());
            Schema<?> resolvedSchema = openAPI.getComponents().getSchemas().get(ref);
            extractTextFromSchema(resolvedSchema, openAPI, sb, visitedRefs, depth + 1);
            return;
        }

        if (schema.getDescription() != null) sb.append(schema.getDescription()).append(" ");
        if (schema.getTitle() != null) sb.append(schema.getTitle()).append(" ");

        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                sb.append(entry.getKey()).append(" ");
                extractTextFromSchema((Schema<?>) entry.getValue(), openAPI, sb, visitedRefs, depth + 1);
            }
        } else if ("array".equals(schema.getType()) && schema.getItems() != null) {
            extractTextFromSchema(schema.getItems(), openAPI, sb, visitedRefs, depth + 1);
        }
    }
}