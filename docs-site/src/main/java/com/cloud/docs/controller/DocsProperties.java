package com.cloud.docs.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps to configuration properties under the 'docs.api' prefix.
 * This allows for type-safe and externalized configuration of the documentation site.
 */
@Component
@ConfigurationProperties(prefix = "docs.api")
@Getter
@Setter
public class DocsProperties {

    /**
     * A list of remote URLs to fetch OpenAPI specifications from.
     * <p>
     * Example in application.properties:
     * <pre>
     * docs.api.spec-urls[0]=http://localhost:8081/spec.yaml
     * docs.api.spec-urls[1]=http://localhost:8082/spec.yaml
     * </pre>
     */
    private List<String> specUrls = new ArrayList<>();
}