package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiRegistryService {

    private final List<ApiSpecification> specifications;
    private final Map<String, ApiSpecification> specsById;

    public ApiRegistryService(List<ApiSpecification> loadedSpecifications) {
        // Sort specs for a predictable order in the UI
        loadedSpecifications.sort(Comparator.comparing(ApiSpecification::title)
                .thenComparing(ApiSpecification::version));

        this.specifications = Collections.unmodifiableList(loadedSpecifications);

        // Create a map for quick lookups by the unique ID
        this.specsById = this.specifications.stream()
                .collect(Collectors.toMap(
                        ApiSpecification::id,
                        spec -> spec,
                        (s1, s2) -> s1, // In case of ID collision, take the first one
                        LinkedHashMap::new
                ));
    }

    /**
     * Returns a collection of all loaded API specifications.
     */
    public Collection<ApiSpecification> getSpecifications() {
        return specifications;
    }

    /**
     * Retrieves a specific API specification by its unique ID.
     */
    public Optional<ApiSpecification> getSpecification(String id) {
        return Optional.ofNullable(specsById.get(id));
    }

    /**
     * Gets the first API in the sorted list, used as the default on page load.
     */
    public Optional<ApiSpecification> getFirstApi() {
        return specifications.stream().findFirst();
    }
}