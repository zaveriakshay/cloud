package com.cloud.docs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public List<ApiSpecification> apiSpecifications() throws IOException {
        log.info("Scanning for OpenAPI specifications in 'static/api/' and its subdirectories...");
        List<ApiSpecification> specifications = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:static/api/**/*.yaml");

        // Get the base path to calculate relative paths against
        Path apiBaseDirPath = Paths.get(resolver.getResource("classpath:static/api/").getURI());

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        for (Resource resource : resources) {
            try {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                OpenAPI openAPI = parser.readContents(content, null, options).getOpenAPI();

                if (openAPI != null && openAPI.getInfo() != null) {
                    String title = openAPI.getInfo().getTitle();
                    String version = openAPI.getInfo().getVersion();
                    String fileName = resource.getFilename();

                    // Determine the sub-path relative to the 'api' directory
                    Path resourcePath = Paths.get(resource.getURI());
                    Path relativePath = apiBaseDirPath.relativize(resourcePath);
                    Path parentDir = relativePath.getParent();
                    String subPath = (parentDir != null) ? parentDir.toString().replace('\\', '/') : "";

                    if (title != null && version != null && fileName != null) {
                        specifications.add(new ApiSpecification(title, version, openAPI, fileName, content, subPath));
                        log.info("Successfully loaded API spec: '{}' v'{}' from sub-path '{}' in {}", title, version, subPath, resource.getURI());
                    } else {
                        log.warn("Skipping file {} as it is missing a title or version.", resource.getFilename());
                    }
                } else {
                    log.warn("Could not parse OpenAPI spec from file: {}", resource.getFilename());
                }
            } catch (Exception e) {
                log.error("Failed to load or parse API specification: {}", resource.getFilename(), e);
            }
        }

        if (specifications.isEmpty()) {
            log.error("No OpenAPI specifications found! The application may not function correctly.");
        }
        return specifications;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }
}