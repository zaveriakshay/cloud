package com.cloud.docs.controller;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        // Add extensions for GitHub Flavored Markdown (tables, strikethrough, etc.)
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), StrikethroughExtension.create());

        this.parser = Parser.builder()
                .extensions(extensions)
                .build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
    }

    public String getRenderedHTML(String pageName) throws IOException {
        String filePath = "docs/" + pageName + ".md";
        ClassPathResource resource = new ClassPathResource(filePath);

        if (!resource.exists()) {
            return "<h2>Page not found</h2><p>The documentation for this topic could not be found.</p>";
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            String markdownContent = FileCopyUtils.copyToString(reader);
            Node document = parser.parse(markdownContent);
            return renderer.render(document);
        }
    }
}