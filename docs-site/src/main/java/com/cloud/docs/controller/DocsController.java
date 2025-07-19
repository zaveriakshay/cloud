package com.cloud.docs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
class DocsController {

    private final MarkdownService markdownService;
    private final SnippetService snippetService;

    public DocsController(MarkdownService markdownService, SnippetService snippetService) {
        this.markdownService = markdownService;
        this.snippetService = snippetService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/docs")
    public String index(Model model) {
        String defaultPage = "get-started";
        try {
            model.addAttribute("content", markdownService.getRenderedHTML(defaultPage));
        } catch (IOException e) {
            model.addAttribute("content", "<h2>Error</h2><p>Could not load introductory content.</p>");
        }
        model.addAttribute("currentPage", defaultPage);
        model.addAttribute("snippet", snippetService.generateSnippet(defaultPage, "shell", "curl"));
        model.addAttribute("language", "bash"); // Add language for initial load
        model.addAttribute("apiOperations", snippetService.getApiOperations());

        return "index";
    }

    @GetMapping("/docs/{page}")
    public String getDocsPage(@PathVariable String page, Model model) {
        try {
            model.addAttribute("content", markdownService.getRenderedHTML(page));
        } catch (IOException e) {
            model.addAttribute("content", "<h2>Error Loading Content</h2><p>The requested documentation page could not be loaded.</p>");
        }
        model.addAttribute("currentPage", page);
        model.addAttribute("snippet", snippetService.generateSnippet(page, "shell", "curl"));
        model.addAttribute("language", "bash"); // Add language for subsequent page loads
        return "fragments :: doc_and_code_example";
    }

    @GetMapping("/snippet")
    public String getSnippet(@RequestParam String page, @RequestParam String lang, Model model) {
        String[] parts = lang.split("_");
        String target = parts[0];
        String client = parts.length > 1 ? parts[1] : "";
        model.addAttribute("snippet", snippetService.generateSnippet(page, target, client));
        model.addAttribute("language", getPrismLanguage(target)); // Add specific language for the new snippet
        return "fragments :: code_snippet_wrapper"; // Return the new, more complete fragment
    }

    /**
     * Helper method to map our internal language names to Prism.js CSS classes.
     */
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
            default -> "clike"; // A safe default for unknown languages
        };
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login";
    }
}