package com.cloud.docs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public List<Extension> commonmarkExtensions() {
        return List.of(TablesExtension.create(), StrikethroughExtension.create());
    }

    @Bean
    public Parser markdownParser(List<Extension> extensions) {
        return Parser.builder().extensions(extensions).build();
    }

    @Bean
    public HtmlRenderer htmlRenderer(List<Extension> extensions) {
        return HtmlRenderer.builder().extensions(extensions).build();
    }

    @Bean
    public List<ApiSpecification> apiSpecifications(DocsProperties docsProperties) {
        log.info("Loading OpenAPI specifications from remote endpoints configured in application.properties...");
        List<ApiSpecification> specifications = new ArrayList<>();
        List<String> specUrls = docsProperties.getSpecUrls();

        if (specUrls.isEmpty()) {
            log.warn("No remote API spec URLs configured under 'docs.api.spec-urls'. No documentation will be loaded.");
            return specifications;
        }

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        for (String url : specUrls) {
            try {
                log.info("Attempting to load spec from: {}", url);

                // Step 1: Fetch the raw content from the URL first.
                String rawContent;
                try (InputStream inputStream = new URL(url).openStream()) {
                    rawContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("Failed to fetch content from URL: {}. Skipping.", url, e);
                    continue; // Move to the next URL
                }

                // FIX: Create a new parser for each spec to ensure no state is carried over.
                OpenAPIV3Parser parser = new OpenAPIV3Parser();

                // Step 2: Parse the fetched content string to get the detailed result.
                SwaggerParseResult parseResult = parser.readContents(rawContent, null, options);
                OpenAPI openAPI = (parseResult != null) ? parseResult.getOpenAPI() : null;

                if (openAPI != null && openAPI.getInfo() != null) {
                    String title = openAPI.getInfo().getTitle();
                    String version = openAPI.getInfo().getVersion();
                    String fileName = url.substring(url.lastIndexOf('/') + 1);

                    if (title != null && version != null) {
                        // FIX: Pass the source URL to the ApiSpecification constructor
                        specifications.add(new ApiSpecification(title, version, openAPI, fileName, rawContent, url));
                        log.info("Successfully loaded API spec: '{}' v'{}' from URL {}", title, version, url);
                    } else {
                        log.warn("Skipping URL {} as the parsed spec is missing a title or version.", url);
                    }
                } else {
                    String errorMessages = (parseResult != null && parseResult.getMessages() != null)
                            ? String.join(", ", parseResult.getMessages())
                            : "Unknown parsing error.";
                    log.warn("Could not parse OpenAPI spec from URL: {}. Errors: {}", url, errorMessages);
                }
            } catch (Exception e) {
                log.error("A critical error occurred while processing URL: {}", url, e);
            }
        }

        if (specifications.isEmpty()) {
            log.error("No OpenAPI specifications could be loaded from the remote endpoints! The application may not function correctly.");
        }
        return specifications;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Bean
    public YAMLMapper yamlMapper() {
        return new YAMLMapper();
    }
}