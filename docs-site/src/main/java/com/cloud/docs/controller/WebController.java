package com.cloud.docs.controller;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final ApiRegistryService apiRegistry;
    private final SnippetService snippetService;
    // Inject YAMLMapper to parse and modify the spec
    private final YAMLMapper yamlMapper;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Serves the raw OpenAPI spec content by acting as a live proxy.
     * This endpoint now parses the spec and removes all 'description' fields
     * to provide a cleaner view in the API tester.
     *
     * @param apiId The unique ID of the API specification.
     * @return A ResponseEntity containing the modified YAML spec content.
     */
    @GetMapping("/api-spec")
    public ResponseEntity<String> getApiSpec(@RequestParam String apiId) {

        Optional<ApiSpecification> specOptional = apiRegistry.getSpecification(apiId);

        if (specOptional.isEmpty()) {
            log.warn("Spec not found for apiId '{}' in /api-spec endpoint.", apiId);
            return ResponseEntity.notFound().build();
        }

        ApiSpecification spec = specOptional.get();

        try {
            log.info("Fetching latest spec from URL: {}", spec.specUrl());
            String rawYamlContent;
            try (InputStream inputStream = new URL(spec.specUrl()).openStream()) {
                rawYamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Parse the YAML, remove all description fields, and convert it back to a string
            Object yamlObject = yamlMapper.readValue(rawYamlContent, Object.class);
            removeDescriptions(yamlObject); // Use the improved removal method
            String modifiedYamlContent = yamlMapper.writeValueAsString(yamlObject);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/yaml;charset=UTF-8"))
                    .body(modifiedYamlContent);

        } catch (IOException e) {
            log.error("Failed to fetch or modify spec from URL '{}' for apiId: {}", spec.specUrl(), apiId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("/* Failed to fetch or modify spec from source: " + e.getMessage() + " */");
        }
    }

    /**
     * FIX: Recursively traverses a parsed YAML/JSON object and completely REMOVES 'description' fields.
     * This is more robust than setting the value to an empty string.
     *
     * @param node The current object node (can be a Map or a List).
     */
    private void removeDescriptions(Object node) {
        if (node instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) node;
            // Use an iterator to safely remove elements from the map while traversing it.
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if ("description".equals(entry.getKey())) {
                    iterator.remove(); // Remove the key-value pair entirely
                } else {
                    // Otherwise, continue traversing deeper into the structure.
                    removeDescriptions(entry.getValue());
                }
            }
        } else if (node instanceof List) {
            // If the node is a list, iterate over its elements and recurse.
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) node;
            list.forEach(this::removeDescriptions);
        }
    }

    /**
     * Serves the RapiDoc tester page.
     * This method provides the URL to the proxied spec and the necessary details for deep-linking.
     *
     * @param apiId       The unique ID of the API specification.
     * @param operationId The ID of the operation to deep-link to.
     * @param model       The Spring UI model.
     * @return The name of the view template.
     */
    @GetMapping("/rapidoc-tester")
    public String rapidocTester(
            @RequestParam String apiId,
            @RequestParam(name = "operation") String operationId,
            Model model) {

        String specUrl = UriComponentsBuilder.fromPath("/api-spec")
                .queryParam("apiId", apiId)
                .build()
                .toUriString();

        model.addAttribute("specUrl", specUrl);

        apiRegistry.getSpecification(apiId)
                .flatMap(spec -> snippetService.findOperationDetailsById(spec.openAPI(), operationId))
                .ifPresent(opDetails -> {
                    model.addAttribute("httpPath", opDetails.path());
                    model.addAttribute("httpMethod", opDetails.method().toString().toLowerCase());
                    log.debug("Passing deep-link info to rapidoc-tester: path={}, method={}", opDetails.path(), opDetails.method().toString().toLowerCase());
                });

        return "rapidoc-tester";
    }
}