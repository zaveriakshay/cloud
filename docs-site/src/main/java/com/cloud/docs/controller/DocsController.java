package com.cloud.docs.controller;

import lombok.RequiredArgsConstructor;
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
    private final MarkdownService markdownService;
    private final SnippetService snippetService;
    private final ResponseService responseService;

    @GetMapping("/")
    public String root() {
        return "redirect:/docs";
    }

    @GetMapping("/docs")
    public String index(Model model) throws IOException {
        // 1. Get all available APIs for the sidebar dropdowns
        model.addAttribute("availableApis", apiRegistry.getAvailableApis());

        // 2. Determine the default API to display on first load
        Optional<ApiSpecification> defaultApiOptional = apiRegistry.getFirstApi();
        if (defaultApiOptional.isEmpty()) {
            log.error("No APIs found. The documentation site cannot be rendered.");
            model.addAttribute("error", "No API specifications found in static/api/");
            return "error"; // Or a specific error template
        }
        ApiSpecification defaultApi = defaultApiOptional.get();
        String defaultApiTitle = defaultApi.title();
        String defaultApiVersion = defaultApi.version();

        model.addAttribute("defaultApiTitle", defaultApiTitle);
        model.addAttribute("defaultApiVersion", defaultApiVersion);

        // 3. Load content for the default API
        loadPageContent(model, defaultApiTitle, defaultApiVersion);

        return "index";
    }

    /**
     * Dynamically loads the navigation links for a specific API version.
     * Called by HTMX when an accordion is opened or a version is changed.
     */
    @GetMapping("/sidebar-nav")
    public String getSidebarNav(@RequestParam String title, @RequestParam String version, Model model) {
        model.addAttribute("groupedOperations", snippetService.getGroupedApiOperations(title, version));
        return "fragments :: sidebar_nav_section";
    }

    /**
     * Dynamically loads the main content area (markdown docs) for a specific API version.
     * Called by HTMX when the version is changed.
     */
    @GetMapping("/main-content")
    public String getMainContent(@RequestParam String title, @RequestParam String version, Model model) throws IOException {
        loadPageContent(model, title, version);
        return "fragments :: main_content_pane";
    }

    /**
     * Dynamically loads a code snippet for a specific operation.
     */
    @GetMapping("/snippet")
    public String getSnippet(
            @RequestParam String apiTitle,
            @RequestParam String apiVersion,
            @RequestParam String page, // This is the operationId
            @RequestParam String lang,
            Model model) {

        String[] parts = lang.split("_");
        String target = parts[0];
        String client = parts.length > 1 ? parts[1] : "";

        model.addAttribute("snippet", snippetService.generateSnippet(apiTitle, apiVersion, page, target, client));
        model.addAttribute("language", getPrismLanguage(target));
        return "fragments :: code_snippet_wrapper";
    }

    /**
     * Dynamically loads response examples for a specific operation.
     */
    @GetMapping("/responses")
    public String getResponses(
            @RequestParam String apiTitle,
            @RequestParam String apiVersion,
            @RequestParam String page, // This is the operationId
            Model model) {
        model.addAttribute("responses", responseService.getResponsesForOperation(apiTitle, apiVersion, page));
        return "fragments :: response_section";
    }

    /**
     * Helper method to load all necessary content for a given API version into the model.
     * This now uses the API's sub-path to fetch version-specific markdown.
     */
    private void loadPageContent(Model model, String title, String version) throws IOException {
        // Get the full ApiSpecification object to access its subPath
        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getApiSpecification(title, version);

        if (apiSpecOptional.isEmpty()) {
            log.error("Could not find API for title '{}' and version '{}' during page load.", title, version);
            // You might want to add error handling here, e.g., by adding an error to the model
            return;
        }
        ApiSpecification apiSpec = apiSpecOptional.get();

        // Get operations for the sidebar
        Map<String, List<OperationInfo>> groupedOperations = snippetService.getGroupedApiOperations(title, version);
        model.addAttribute("groupedOperations", groupedOperations);

        // Get rendered markdown specific to this API's sub-path
        Map<String, String> allDocsUnordered = markdownService.getRenderedHtmlForApi(apiSpec.subPath());
        Map<String, String> allDocsOrdered = new LinkedHashMap<>();

        // Always add "get-started" first if it exists
        if (allDocsUnordered.containsKey("get-started")) {
            allDocsOrdered.put("get-started", allDocsUnordered.get("get-started"));
        } else {
            log.warn("The 'get-started.md' file is missing and will not be displayed.");
        }

        // Add docs for the operations in the currently selected API, maintaining order
        groupedOperations.values().stream()
                .flatMap(List::stream)
                .forEach(opInfo -> {
                    String pageName = opInfo.id();
                    if (allDocsUnordered.containsKey(pageName)) {
                        allDocsOrdered.put(pageName, allDocsUnordered.get(pageName));
                    } else {
                        log.warn("Markdown file for operation '{}' not found for API '{}' v'{}'. It will be skipped.", pageName, title, version);
                    }
                });

        model.addAttribute("allDocs", allDocsOrdered);

        // Provide initial state for the right pane, defaulting to the 'get-started' page
        String firstPage = "get-started";
        model.addAttribute("currentPage", firstPage);
        model.addAttribute("snippet", snippetService.generateSnippet(title, version, firstPage, "shell", "curl"));
        model.addAttribute("language", "bash");
        model.addAttribute("responses", responseService.getResponsesForOperation(title, version, firstPage));
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