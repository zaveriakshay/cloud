package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This configuration class is responsible for loading all remote OpenAPI specifications
 * defined in the application properties. It creates a list of ApiSpecification beans
 * that can then be injected into the ApiRegistryService.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApiLoaderConfig {

    private final DocsProperties docsProperties;

    /**
     * Creates a list of ApiSpecification beans by parsing each URL from the properties.
     * This list is then injected by Spring into the ApiRegistryService constructor.
     *
     * @return A list of fully parsed and loaded ApiSpecification objects.
     */
    @Bean
    public List<ApiSpecification> loadedSpecifications() {
        List<ApiSpecification> specs = new ArrayList<>();
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        log.info("Found {} spec URLs to load from properties.", docsProperties.getSpecUrls().size());

        for (String url : docsProperties.getSpecUrls()) {
            try {
                log.info("Attempting to load API specification from: {}", url);
                ParseOptions options = new ParseOptions();
                options.setResolve(true); // Important for resolving $refs

                // Use the Swagger Parser to read the spec from the URL
                SwaggerParseResult result = parser.readLocation(url, null, options);
                OpenAPI openAPI = result.getOpenAPI();

                if (openAPI == null) {
                    log.error("Failed to parse OpenAPI spec from [{}]. Errors: {}", url, result.getMessages());
                    continue; // Skip to the next URL
                }

                // The parser doesn't retain the raw content, so we fetch it again to store it.
                String rawContent;
                try (InputStream inputStream = new URL(url).openStream()) {
                    rawContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }

                String title = (openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null)
                        ? openAPI.getInfo().getTitle()
                        : "Untitled API";
                String version = (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null)
                        ? openAPI.getInfo().getVersion()
                        : "1.0";
                String fileName = new URL(url).getPath().substring(new URL(url).getPath().lastIndexOf('/') + 1);

                // Create the specification object and add it to our list
                ApiSpecification spec = new ApiSpecification(title, version, openAPI, fileName, rawContent, url);
                specs.add(spec);
                log.info("Successfully loaded API: '{}' version '{}' with ID '{}'", spec.title(), spec.version(), spec.id());

            } catch (Exception e) {
                log.error("Exception occurred while loading API specification from URL: {}", url, e);
            }
        }
        log.info("Total API specifications loaded: {}", specs.size());
        return specs;
    }
}