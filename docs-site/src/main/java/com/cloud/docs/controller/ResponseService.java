package com.cloud.docs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseService {

    private final ApiRegistryService apiRegistry;
    private final ObjectMapper objectMapper;

    private record OperationDetails(String path, PathItem.HttpMethod method, Operation operation) {}
    public record ResponseInfo(String description, String exampleJson) {}

    /**
     * Extracts all response examples for a given operation, now requiring API context.
     *
     * @param apiTitle The title of the API spec.
     * @param apiVersion The version of the API spec.
     * @param operationId The ID of the operation to get responses for.
     * @return A map of status codes to their corresponding response information.
     */
    public Map<String, ResponseInfo> getResponsesForOperation(String apiTitle, String apiVersion, String operationId) {
        Map<String, ResponseInfo> responseExamples = new TreeMap<>();

        if ("get-started".equals(operationId)) {
            return responseExamples; // No responses for the intro page
        }

        // 1. Get the correct OpenAPI spec from the registry
        Optional<OpenAPI> openApiOptional = apiRegistry.getApi(apiTitle, apiVersion);
        if (openApiOptional.isEmpty()) {
            log.warn("Cannot get responses. OpenAPI spec not found for: {} v{}", apiTitle, apiVersion);
            return responseExamples;
        }
        OpenAPI openAPI = openApiOptional.get();

        // 2. Find the operation within that specific spec
        Optional<OperationDetails> opDetailsOptional = findOperationDetailsById(openAPI, operationId);

        if (opDetailsOptional.isEmpty() || opDetailsOptional.get().operation().getResponses() == null) {
            return responseExamples;
        }

        // 3. Process responses as before
        opDetailsOptional.get().operation().getResponses().forEach((statusCode, apiResponse) -> {
            // Pass the openAPI object to the resolver
            ApiResponse resolvedResponse = resolveResponseRef(openAPI, apiResponse);
            if (resolvedResponse == null) return;

            String description = resolvedResponse.getDescription();
            String exampleJson = extractExampleJsonFromResponse(resolvedResponse);

            responseExamples.put(statusCode, new ResponseInfo(description, exampleJson));
        });

        return responseExamples;
    }

    /**
     * Follows a $ref link in a response to get the actual ApiResponse object from the correct spec.
     */
    private ApiResponse resolveResponseRef(OpenAPI openAPI, ApiResponse response) {
        if (response.get$ref() != null) {
            String ref = response.get$ref().substring("#/components/responses/".length());
            // Use the passed-in openAPI object
            return openAPI.getComponents().getResponses().get(ref);
        }
        return response;
    }

    /**
     * Extracts a pretty-printed JSON example from an ApiResponse object.
     */
    private String extractExampleJsonFromResponse(ApiResponse response) {
        Content content = response.getContent();
        if (content == null || !content.containsKey("application/json")) {
            return "{\n  \"message\": \"" + StringUtils.capitalize(response.getDescription()) + "\"\n}";
        }

        MediaType mediaType = content.get("application/json");
        if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            Example example = mediaType.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                try {
                    // Use the injected objectMapper bean
                    return objectMapper.writeValueAsString(example.getValue());
                } catch (JsonProcessingException e) {
                    log.error("Error serializing response example JSON", e);
                    return "{\n  \"error\": \"Could not generate response example.\"\n}";
                }
            }
        }
        return "{\n  \"message\": \"No example available for this response.\"\n}";
    }

    /**
     * Finds an operation by its operationId within a specific OpenAPI spec.
     */
    private Optional<OperationDetails> findOperationDetailsById(OpenAPI openAPI, String operationId) {
        if (openAPI.getPaths() == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet()) {
                Operation operation = opEntry.getValue();
                if (operation != null && operationId.equals(operation.getOperationId())) {
                    return Optional.of(new OperationDetails(pathEntry.getKey(), opEntry.getKey(), operation));
                }
            }
        }
        return Optional.empty();
    }
}