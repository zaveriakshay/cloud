package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationContentService {

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    /**
     * Extracts markdown from the description fields within an OpenAPI object
     * and renders it to HTML.
     *
     * @param openApi The parsed OpenAPI specification object.
     * @return A map where the key is the page name (operationId or "get-started")
     *         and the value is the rendered HTML content.
     */
    public Map<String, String> getRenderedHtmlForApi(OpenAPI openApi) {
        // Get the title for logging, handle null safely.
        final String apiTitle = (openApi.getInfo() != null && openApi.getInfo().getTitle() != null)
                ? openApi.getInfo().getTitle()
                : "Unknown API";

        log.info("Extracting documentation content for API: '{}'", apiTitle);

        Map<String, String> renderedDocs = new HashMap<>();

        // 1. Process the main info.description for the "get-started" page.
        if (openApi.getInfo() != null && openApi.getInfo().getDescription() != null) {
            String infoMarkdown = openApi.getInfo().getDescription();
            renderedDocs.put("get-started", renderMarkdownToHtml(infoMarkdown));
        }

        // 2. Process the description for each operation.
        if (openApi.getPaths() != null) {
            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (operation != null && operation.getOperationId() != null && operation.getDescription() != null) {
                        String operationId = operation.getOperationId();
                        String operationMarkdown = operation.getDescription();
                        String renderedHtml = renderMarkdownToHtml(operationMarkdown);

                        // This check prevents overwrites if an operationId is duplicated within a single spec file.
                        if (renderedDocs.containsKey(operationId)) {
                            log.warn("Duplicate operationId '{}' found in API '{}'. The content for the first one found will be used.",
                                    operationId, apiTitle);
                        }
                        // Use putIfAbsent to ensure the first one wins, maintaining consistency.
                        renderedDocs.putIfAbsent(operationId, renderedHtml);
                    }
                });
            });
        }

        return renderedDocs;
    }

    private String renderMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }
}