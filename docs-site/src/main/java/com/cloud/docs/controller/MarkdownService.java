package com.cloud.docs.controller;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService(Parser parser, HtmlRenderer renderer) {
        this.parser = parser;
        this.renderer = renderer;
    }

    /**
     * Scans for .md files for a given API context, renders them to HTML, and returns a map.
     * It layers root-level docs with version-specific docs.
     *
     * @param apiSubPath The sub-path (e.g., "v1") to look for version-specific docs.
     * @return A map of page names to rendered HTML content.
     */
    public Map<String, String> getRenderedHtmlForApi(String apiSubPath) throws IOException {
        Map<String, String> renderedDocs = new LinkedHashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 1. Load common docs from the root `docs/` directory first.
        Resource[] rootResources = resolver.getResources("classpath:docs/*.md");
        addResourcesToMap(rootResources, renderedDocs);

        // 2. If a sub-path is provided, load version-specific docs,
        // which will overwrite any common docs with the same name.
        if (apiSubPath != null && !apiSubPath.isBlank()) {
            String specificPath = "classpath:docs/" + apiSubPath + "/*.md";
            try {
                Resource[] specificResources = resolver.getResources(specificPath);
                addResourcesToMap(specificResources, renderedDocs);
            } catch (IOException e) {
                // It's okay if a version-specific doc folder doesn't exist
            }
        }

        return renderedDocs;
    }

    private void addResourcesToMap(Resource[] resources, Map<String, String> map) throws IOException {
        // Sort to ensure consistent order
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null) {
                String pageName = filename.replace(".md", "");
                String markdownContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String htmlContent = renderMarkdownToHtml(markdownContent);
                map.put(pageName, htmlContent);
            }
        }
    }

    private String renderMarkdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}