package com.cloud.docs.controller;

import lombok.RequiredArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DocsController {

    private static final Logger log = LoggerFactory.getLogger(DocsController.class);

    private final ApiRegistryService apiRegistry;
    private final SnippetService snippetService;
    private final ResponseService responseService;
    // Inject the markdown beans directly
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    @GetMapping("/")
    public String root() {
        return "redirect:/docs";
    }

    /**
     * REFACTORED: The main entry point now accepts an 'apiId' to determine which API to display.
     * This enables tab-based navigation with full page reloads.
     */
    @GetMapping("/docs")
    public String index(@RequestParam(name = "apiId", required = false) String apiId, Model model) throws IOException {
        // Pass all loaded APIs to the view for the top navigation tabs
        model.addAttribute("allApis", apiRegistry.getSpecifications());

        // Determine which API to load based on the request parameter or the default
        Optional<ApiSpecification> apiToLoadOptional = (apiId != null)
                ? apiRegistry.getSpecification(apiId)
                : apiRegistry.getFirstApi();

        // If the requested apiId is invalid or no APIs exist, fall back gracefully.
        if (apiToLoadOptional.isEmpty()) {
            // Try getting the first one again as a safe fallback
            apiToLoadOptional = apiRegistry.getFirstApi();
            if (apiToLoadOptional.isEmpty()) {
                log.error("No APIs found. The documentation site cannot be rendered.");
                model.addAttribute("error", "No API specifications found.");
                return "error";
            }
        }
        ApiSpecification currentApi = apiToLoadOptional.get();

        // Pass the ID of the currently active API to the view for styling the active tab
        // and initializing the Alpine.js state.
        model.addAttribute("currentApiId", currentApi.id());

        // Load all page content (sidebar, main docs, etc.) for the selected API
        loadPageContent(model, currentApi.id());

        return "index";
    }

    @GetMapping("/sidebar-nav")
    public String getSidebarNav(@RequestParam String apiId, Model model) {
        model.addAttribute("groupedOperations", snippetService.getGroupedApiOperations(apiId));
        return "fragments :: sidebar_nav_section";
    }

    @GetMapping("/main-content")
    public String getMainContent(@RequestParam String apiId, Model model) throws IOException {
        loadPageContent(model, apiId);
        return "fragments :: main_content_pane";
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
        return "fragments :: code_snippet_wrapper";
    }

    @GetMapping("/responses")
    public String getResponses(
            @RequestParam String apiId,
            @RequestParam String page,
            Model model) {
        model.addAttribute("responses", responseService.getResponsesForOperation(apiId, page));
        return "fragments :: response_section";
    }

    /**
     * REFACTORED: This method now uses a single source of truth (SnippetService)
     * to build the documentation content, making it more robust.
     */
    private void loadPageContent(Model model, String apiId) throws IOException {
        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getSpecification(apiId);
        if (apiSpecOptional.isEmpty()) {
            log.error("Could not find API for id '{}' during page load.", apiId);
            return;
        }
        ApiSpecification apiSpec = apiSpecOptional.get();

        // 1. Get the list of operations. This is our reliable source of truth.
        Map<String, List<OperationInfo>> groupedOperations = snippetService.getGroupedApiOperations(apiId);
        model.addAttribute("groupedOperations", groupedOperations);

        Map<String, String> allDocsOrdered = new LinkedHashMap<>();

        // 2. Process the main "get-started" page from the spec's info.description
        if (apiSpec.openAPI().getInfo() != null && apiSpec.openAPI().getInfo().getDescription() != null) {
            String infoMarkdown = apiSpec.openAPI().getInfo().getDescription();
            allDocsOrdered.put("get-started", renderMarkdownToHtml(infoMarkdown));
        } else {
            log.warn("The 'info.description' for the 'get-started' page is missing in the OpenAPI spec for apiId: {}", apiId);
        }

        // 3. Iterate through the known operations and render their descriptions directly.
        groupedOperations.values().stream()
                .flatMap(List::stream)
                .forEach(opInfo -> {
                    String pageName = opInfo.id();
                    String markdownDescription = opInfo.description();

                    if (markdownDescription != null && !markdownDescription.isBlank()) {
                        allDocsOrdered.put(pageName, renderMarkdownToHtml(markdownDescription));
                    } else {
                        log.warn("Markdown description for operation '{}' not found for API '{}'. It will be skipped.", pageName, apiId);
                    }
                });

        model.addAttribute("allDocs", allDocsOrdered);

        // Set default content for the right-hand panes
        String firstPage = "get-started";
        model.addAttribute("currentPage", firstPage);
        model.addAttribute("snippet", snippetService.generateSnippet(apiId, firstPage, "shell", "curl"));
        model.addAttribute("language", "bash");
        model.addAttribute("responses", responseService.getResponsesForOperation(apiId, firstPage));
    }

    private String renderMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
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

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login";
    }
}