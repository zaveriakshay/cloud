package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * A record to hold a parsed OpenAPI specification and its metadata.
 *
 * @param title      The API title from info.title.
 * @param version    The API version from info.version.
 * @param openAPI    The parsed OpenAPI object.
 * @param fileName   The original filename of the spec.
 * @param rawContent The raw YAML/JSON content of the spec.
 * @param subPath    The sub-path within 'static/api' where the spec was found (e.g., "v1").
 */
public record ApiSpecification(
        String title,
        String version,
        OpenAPI openAPI,
        String fileName,
        String rawContent,
        String subPath
) {}