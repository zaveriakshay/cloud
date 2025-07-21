package com.cloud.docs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DocsController {

    private final ApiRegistryService apiRegistry;
    private final SnippetService snippetService;
    private final ResponseService responseService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final SpaceService spaceService;
    private final DocsHubRegistry docsHubRegistry;

    /**
     * A simple record to hold navigation links for a page.
     */
    public record PageNavigation(PageNode previous, PageNode next) {}

    @GetMapping("/api")
    public String index(
            @RequestParam(name = "apiId", required = false) String apiId,
            Model model) throws IOException {

        model.addAttribute("allApis", apiRegistry.getSpecifications());

        Optional<ApiSpecification> apiToLoadOptional = (apiId != null)
                ? apiRegistry.getSpecification(apiId)
                : apiRegistry.getFirstApi();

        if (apiToLoadOptional.isEmpty()) {
            apiToLoadOptional = apiRegistry.getFirstApi();
            if (apiToLoadOptional.isEmpty()) {
                log.error("No APIs found. The documentation site cannot be rendered.");
                model.addAttribute("error", "No API specifications found.");
                return "error";
            }
        }
        ApiSpecification currentApi = apiToLoadOptional.get();
        model.addAttribute("currentApiId", currentApi.id());
        model.addAttribute("activeTab", currentApi.id());
        model.addAttribute("spaceService", spaceService);
        model.addAttribute("docsHubRegistry", docsHubRegistry);

        loadPageContent(model, currentApi.id());
        return "index";
    }

    @GetMapping("/sidebar-nav")
    public String getSidebarNav(@RequestParam String apiId, Model model) {
        model.addAttribute("groupedOperations", snippetService.getGroupedApiOperations(apiId));
        return "fragments/api-fragments :: sidebar_nav_section";
    }

    @GetMapping("/main-content")
    public String getMainContent(@RequestParam String apiId, Model model) throws IOException {
        loadPageContent(model, apiId);
        return "fragments/api-fragments :: main_content_pane";
    }

    @GetMapping("/snippet")
    public String getSnippet(
            @RequestParam String apiId,
            @RequestParam String page,
            @RequestParam String lang,
            Model model) {

        String[] parts = lang.split("_");
        String target = parts[0];
        String client = parts.length > 1 ? parts[1] : "";

        model.addAttribute("snippet", snippetService.generateSnippet(apiId, page, target, client));
        model.addAttribute("language", getPrismLanguage(target));
        return "fragments/api-fragments :: code_snippet_wrapper";
    }

    @GetMapping("/responses")
    public String getResponses(
            @RequestParam String apiId,
            @RequestParam String page,
            Model model) {
        model.addAttribute("responses", responseService.getResponsesForOperation(apiId, page));
        return "fragments/api-fragments :: response_section";
    }

    @GetMapping("/space")
    public String getSpacePage(Model model) {
        model.addAttribute("allApis", apiRegistry.getSpecifications());
        model.addAttribute("activeTab", "space");
        model.addAttribute("spaceService", spaceService);
        model.addAttribute("docsHubRegistry", docsHubRegistry);
        return "space";
    }

    @GetMapping("/published-space")
    public String getPublishedSpace(Model model) {
        spaceService.getPublishedPageTree().ifPresent(publishedRoot -> {
            model.addAttribute("workspace", publishedRoot);

            Map<String, String> renderedContent = new LinkedHashMap<>();
            collectRenderedContent(publishedRoot, renderedContent);
            model.addAttribute("renderedContent", renderedContent);

            List<PageNode> flatPages = spaceService.getFlatPageList(publishedRoot);
            Map<String, PageNavigation> pageNavigation = new HashMap<>();
            for (int i = 0; i < flatPages.size(); i++) {
                PageNode prev = (i > 0) ? flatPages.get(i - 1) : null;
                PageNode next = (i < flatPages.size() - 1) ? flatPages.get(i + 1) : null;
                pageNavigation.put(flatPages.get(i).id(), new PageNavigation(prev, next));
            }
            model.addAttribute("pageNavigation", pageNavigation);
        });

        model.addAttribute("allApis", apiRegistry.getSpecifications());
        model.addAttribute("activeTab", "published-space");
        model.addAttribute("spaceService", spaceService);
        model.addAttribute("docsHubRegistry", docsHubRegistry);
        return "published-space";
    }

    // --- Private Helper Methods ---

    private void loadPageContent(Model model, String apiId) throws IOException {
        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getSpecification(apiId);
        if (apiSpecOptional.isEmpty()) {
            log.error("Could not find API for id '{}' during page load.", apiId);
            return;
        }
        ApiSpecification apiSpec = apiSpecOptional.get();

        Map<String, List<OperationInfo>> groupedOperations = snippetService.getGroupedApiOperations(apiId);
        model.addAttribute("groupedOperations", groupedOperations);

        Map<String, String> allDocsOrdered = new LinkedHashMap<>();
        if (apiSpec.openAPI().getInfo() != null && apiSpec.openAPI().getInfo().getDescription() != null) {
            String infoMarkdown = apiSpec.openAPI().getInfo().getDescription();
            allDocsOrdered.put("get-started", renderMarkdownToHtml(infoMarkdown));
        }

        groupedOperations.values().stream()
                .flatMap(List::stream)
                .forEach(opInfo -> {
                    if (opInfo.description() != null && !opInfo.description().isBlank()) {
                        allDocsOrdered.put(opInfo.id(), renderMarkdownToHtml(opInfo.description()));
                    }
                });

        model.addAttribute("allDocs", allDocsOrdered);
        String firstPage = "get-started";
        model.addAttribute("currentPage", firstPage);
        model.addAttribute("snippet", snippetService.generateSnippet(apiId, firstPage, "shell", "curl"));
        model.addAttribute("language", "bash");
        model.addAttribute("responses", responseService.getResponsesForOperation(apiId, firstPage));
    }

    private void collectRenderedContent(PageNode node, Map<String, String> contentMap) {
        if (!node.isSection() && node.content() != null) {
            String processedMarkdown = unindentMarkdown(node.content());
            contentMap.put(node.id(), renderMarkdownToHtml(processedMarkdown));
        }
        node.children().forEach(child -> collectRenderedContent(child, contentMap));
    }

    private String renderMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }

    private String unindentMarkdown(String markdown) {
        List<String> lines = markdown.lines().collect(Collectors.toList());
        if (lines.isEmpty()) {
            return "";
        }

        String firstLineIndentation = null;
        for (String line : lines) {
            if (!line.isBlank()) {
                int indentLevel = 0;
                while (indentLevel < line.length() && Character.isWhitespace(line.charAt(indentLevel))) {
                    indentLevel++;
                }
                firstLineIndentation = line.substring(0, indentLevel);
                break;
            }
        }

        if (firstLineIndentation == null || firstLineIndentation.isEmpty()) {
            return markdown;
        }

        final String indentToRemove = firstLineIndentation;
        return lines.stream()
                .map(line -> line.startsWith(indentToRemove) ? line.substring(indentToRemove.length()) : line)
                .collect(Collectors.joining("\n"));
    }

    private String getPrismLanguage(String target) {
        return switch (target.toLowerCase()) {
            case "shell" -> "bash";
            case "javascript" -> "javascript";
            case "python" -> "python";
            case "java" -> "java";
            case "csharp" -> "csharp";
            case "php" -> "php";
            case "go" -> "go";
            case "ruby" -> "ruby";
            case "swift" -> "swift";
            case "kotlin" -> "kotlin";
            default -> "clike";
        };
    }
}