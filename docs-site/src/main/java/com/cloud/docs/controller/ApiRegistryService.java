package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiRegistryService {

    private final List<ApiSpecification> specifications;
    private final Map<String, List<ApiSpecification>> specsByTitle;

    public ApiRegistryService(List<ApiSpecification> specifications) {
        this.specifications = specifications;
        this.specsByTitle = specifications.stream()
                .sorted(Comparator.comparing(ApiSpecification::version).reversed())
                .collect(Collectors.groupingBy(
                        ApiSpecification::title,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public Map<String, List<String>> getAvailableApis() {
        return specsByTitle.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(ApiSpecification::version).collect(Collectors.toList()),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    public Optional<OpenAPI> getApi(String title, String version) {
        return specsByTitle.getOrDefault(title, Collections.emptyList()).stream()
                .filter(spec -> spec.version().equals(version))
                .map(ApiSpecification::openAPI)
                .findFirst();
    }

    /**
     * FIX: New method to get the full ApiSpecification object.
     * This is needed by DocsController to access metadata like the sub-path.
     */
    public Optional<ApiSpecification> getApiSpecification(String title, String version) {
        return specsByTitle.getOrDefault(title, Collections.emptyList()).stream()
                .filter(spec -> spec.version().equals(version))
                .findFirst(); // Returns the full object
    }

    /**
     * Gets the raw, unmodified content of the spec file.
     * This is the robust way to pass the spec to the API tester.
     */
    public Optional<String> getApiRawContent(String title, String version) {
        return specsByTitle.getOrDefault(title, Collections.emptyList()).stream()
                .filter(spec -> spec.version().equals(version))
                .map(ApiSpecification::rawContent)
                .findFirst();
    }

    public Optional<ApiSpecification> getFirstApi() {
        return specifications.stream().findFirst();
    }
}