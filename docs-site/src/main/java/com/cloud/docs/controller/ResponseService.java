package com.cloud.docs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseService {

    private final ApiRegistryService apiRegistry;
    private final SnippetService snippetService; // Used to find operations efficiently
    private final ObjectMapper objectMapper;

    /**
     * Gets a map of response examples for a given operation.
     *
     * @param apiId       The unique ID of the API specification.
     * @param operationId The ID of the operation (e.g., "create-payment").
     * @return A map where the key is the HTTP status code and the value contains the response details.
     */
    public Map<String, ResponseInfo> getResponsesForOperation(String apiId, String operationId) {
        if ("get-started".equals(operationId)) {
            return Collections.emptyMap();
        }

        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getSpecification(apiId);
        if (apiSpecOptional.isEmpty()) {
            log.warn("Cannot get responses. API spec not found for id: {}", apiId);
            return Collections.emptyMap();
        }
        OpenAPI openAPI = apiSpecOptional.get().openAPI();

        Optional<SnippetService.OperationDetails> opDetailsOptional = snippetService.findOperationDetailsById(openAPI, operationId);
        if (opDetailsOptional.isEmpty()) {
            log.warn("Cannot get responses. Operation not found for id: {}", operationId);
            return Collections.emptyMap();
        }
        Operation operation = opDetailsOptional.get().operation();

        Map<String, ResponseInfo> responseExamples = new LinkedHashMap<>();
        if (operation.getResponses() == null) {
            return Collections.emptyMap();
        }

        // Iterate through each defined response (e.g., "200", "404")
        operation.getResponses().forEach((statusCode, response) -> {
            ApiResponse resolvedResponse = response;
            // Resolve $ref for ApiResponse if it exists (e.g., #/components/responses/NotFound)
            if (response.get$ref() != null) {
                String ref = response.get$ref().substring("#/components/responses/".length());
                resolvedResponse = openAPI.getComponents().getResponses().get(ref);
            }

            if (resolvedResponse != null) {
                String description = resolvedResponse.getDescription();
                String exampleJson = "{}"; // Default to empty JSON

                if (resolvedResponse.getContent() != null && resolvedResponse.getContent().containsKey("application/json")) {
                    MediaType mediaType = resolvedResponse.getContent().get("application/json");
                    exampleJson = generateExampleForResponse(mediaType, openAPI);
                }
                responseExamples.put(statusCode, new ResponseInfo(description, exampleJson));
            }
        });

        return responseExamples;
    }

    /**
     * Generates an example JSON string for a given response MediaType.
     * It prioritizes explicit examples and falls back to schema-based generation.
     */
    private String generateExampleForResponse(MediaType mediaType, OpenAPI openAPI) {
        // Priority 1: Use explicit examples from the spec
        if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            Example example = mediaType.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                return prettyPrintJson(example.getValue());
            }
        }

        // Priority 2: Generate a payload from the schema
        if (mediaType.getSchema() != null) {
            Object generatedExample = generateExampleFromSchema(mediaType.getSchema(), openAPI, new HashSet<>());
            if (generatedExample != null) {
                return prettyPrintJson(generatedExample);
            }
        }

        return "{}";
    }

    private String prettyPrintJson(Object object) {
        try {
            // Use the pretty printer for consistent, readable output.
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing example JSON for response", e);
            return "{\n  \"error\": \"Could not generate example payload.\"\n}";
        }
    }

    /**
     * Recursively generates an example value from a given schema.
     * This logic is mirrored from SnippetService to ensure consistency.
     */
    private Object generateExampleFromSchema(Schema<?> schema, OpenAPI openAPI, Set<String> visitedRefs) {
        // Handle $ref and prevent circular loops
        if (schema.get$ref() != null) {
            if (visitedRefs.contains(schema.get$ref())) {
                return "circular reference";
            }
            visitedRefs.add(schema.get$ref());
            String ref = schema.get$ref().substring("#/components/schemas/".length());
            schema = openAPI.getComponents().getSchemas().get(ref);
        }

        if (schema == null) {
            return null;
        }

        if (schema.getExample() != null) {
            return schema.getExample();
        }
        if (schema.getDefault() != null) {
            return schema.getDefault();
        }

        String type = schema.getType();
        if (type == null) return null;

        return switch (type) {
            case "string" -> generateStringExample(schema);
            case "integer" -> generateIntegerExample(schema);
            case "number" -> generateNumberExample(schema);
            case "boolean" -> true;
            case "object" -> {
                Map<String, Object> exampleJson = new LinkedHashMap<>();
                Map<String, Schema> properties = schema.getProperties();
                if (properties != null) {
                    for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                        exampleJson.put(entry.getKey(), generateExampleFromSchema(entry.getValue(), openAPI, new HashSet<>(visitedRefs)));
                    }
                }
                yield exampleJson;
            }
            case "array" -> {
                Schema<?> itemsSchema = schema.getItems();
                if (itemsSchema != null) {
                    yield List.of(generateExampleFromSchema(itemsSchema, openAPI, visitedRefs));
                }
                yield Collections.emptyList();
            }
            default -> null;
        };
    }

    private String generateStringExample(Schema<?> propertySchema) {
        if (propertySchema.getEnum() != null && !propertySchema.getEnum().isEmpty()) {
            return propertySchema.getEnum().get(0).toString();
        }
        String format = propertySchema.getFormat();
        if (format != null) {
            return switch (format) {
                case "date" -> "2024-07-21";
                case "date-time" -> "2024-07-21T14:30:00Z";
                case "email" -> "user@example.com";
                case "uuid" -> UUID.randomUUID().toString();
                case "uri" -> "https://example.com/resource/123";
                default -> "string";
            };
        }
        return "string";
    }

    private Integer generateIntegerExample(Schema<?> propertySchema) {
        if (propertySchema.getMinimum() != null) {
            return propertySchema.getMinimum().intValue();
        }
        return 1000;
    }

    private Double generateNumberExample(Schema<?> propertySchema) {
        if (propertySchema.getMinimum() != null) {
            return propertySchema.getMinimum().doubleValue();
        }
        return 10.5;
    }
}