package com.cloud.docs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final ApiRegistryService apiRegistry;
    private final SnippetService snippetService;
    private final ObjectMapper objectMapper;

    // Other endpoints that might be in this file
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/keycloak-mock")
    public String keycloakMock() {
        return "keycloak-mock";
    }

    /**
     * FIX: This endpoint was missing. It serves the raw OpenAPI spec content.
     * RapiDoc fetches the spec from this URL, and the 404 error indicates it was not found.
     */
    @GetMapping("/api-spec")
    public ResponseEntity<String> getApiSpec(
            @RequestParam String title,
            @RequestParam String version) {

        Optional<String> specContentOptional = apiRegistry.getApiRawContent(title, version);

        return specContentOptional
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.oai.openapi;charset=UTF-8"))
                        .body(content))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Serves the RapiDoc tester page.
     * This method provides the URL to the full spec and the necessary details for deep-linking.
     */
    @GetMapping("/rapidoc-tester")
    public String rapidocTester(
            @RequestParam String title,
            @RequestParam String version,
            @RequestParam(name = "operation") String operationId,
            Model model) {

        // Construct the full URL to our spec-serving endpoint
        String specUrl = UriComponentsBuilder.fromPath("/api-spec")
                .queryParam("title", title)
                .queryParam("version", version)
                .build()
                .toUriString();

        model.addAttribute("specUrl", specUrl);

        // Find the operation details and pass the HTTP method and path to the model.
        // This is the crucial information needed to construct the correct element ID.
        apiRegistry.getApi(title, version)
                .flatMap(api -> snippetService.findOperationDetailsById(api, operationId))
                .ifPresent(opDetails -> {
                    model.addAttribute("httpPath", opDetails.path()); // e.g., "/payments"
                    model.addAttribute("httpMethod", opDetails.method().toString().toLowerCase()); // e.g., "post"
                });

        return "rapidoc-tester";
    }
}