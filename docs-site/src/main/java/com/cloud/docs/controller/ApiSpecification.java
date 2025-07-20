package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.util.StringUtils;

/**
 * A record to hold all relevant information about a loaded OpenAPI specification.
 *
 * @param title      The title from the spec's info section.
 * @param version    The version from the spec's info section.
 * @param openAPI    The parsed OpenAPI object model.
 * @param fileName   The original filename of the spec.
 * @param rawContent The raw, unmodified content of the spec file.
 * @param id         A URL-friendly unique identifier generated from the title and version.
 * @param specUrl    The original URL from which the specification was loaded.
 */
public record ApiSpecification(
        String title,
        String version,
        OpenAPI openAPI,
        String fileName,
        String rawContent,
        String id,
        String specUrl // Add the source URL
) {
    // Overloaded constructor for convenience, automatically generating the ID.
    public ApiSpecification(String title, String version, OpenAPI openAPI, String fileName, String rawContent, String specUrl) {
        this(title, version, openAPI, fileName, rawContent, generateId(title, version), specUrl);
    }

    private static String generateId(String title, String version) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(version)) {
            return "unknown-api";
        }
        return (title.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                + "-" + version.replaceAll("\\.", ""))
                .replaceAll("--+", "-");
    }
}