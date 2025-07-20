package com.cloud.docs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnippetService {

    private final ApiRegistryService apiRegistry;
    private final ObjectMapper objectMapper;

    public record OperationDetails(String path, PathItem.HttpMethod method, Operation operation) {}

    /**
     * Generates a code snippet for a given operation within a specific API.
     *
     * @param apiId       The unique ID of the API specification.
     * @param operationId The ID of the operation (e.g., "create-payment").
     * @param target      The language target (e.g., "python").
     * @param client      The client library (e.g., "requests").
     * @return The generated code snippet as a string.
     */
    public String generateSnippet(String apiId, String operationId, String target, String client) {
        if ("get-started".equals(operationId)) {
            return "# Select an API and an endpoint to see an example request.";
        }

        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getSpecification(apiId);
        if (apiSpecOptional.isEmpty()) {
            return "# API Specification not found for ID: " + apiId;
        }
        OpenAPI openAPI = apiSpecOptional.get().openAPI();

        Optional<OperationDetails> opDetailsOptional = findOperationDetailsById(openAPI, operationId);
        if (opDetailsOptional.isEmpty()) {
            log.warn("No operation found for ID: {} in API: {}", operationId, apiId);
            return "# Operation not found in the specified OpenAPI spec.";
        }
        OperationDetails opDetails = opDetailsOptional.get();
        String payload = getPayloadForOperation(openAPI, opDetails.operation());

        return switch (target.toLowerCase()) {
            case "shell" -> generateCurlSnippet(openAPI, opDetails, payload);
            case "python" -> generatePythonSnippet(openAPI, opDetails, payload);
            case "javascript" -> generateNodeJsSnippet(openAPI, opDetails, payload);
            case "java" -> generateJavaSnippet(openAPI, opDetails, payload);
            case "csharp" -> generateCSharpSnippet(openAPI, opDetails, payload);
            case "php" -> generatePhpSnippet(openAPI, opDetails, payload);
            case "go" -> generateGoSnippet(openAPI, opDetails, payload);
            case "ruby" -> generateRubySnippet(openAPI, opDetails, payload);
            case "swift" -> generateSwiftSnippet(openAPI, opDetails, payload);
            case "kotlin" -> generateKotlinSnippet(openAPI, opDetails, payload);
            default -> "# Snippet not available for this language.";
        };
    }

    /**
     * Gets API operations grouped by tag for a specific API specification.
     *
     * @param apiId The unique ID of the API specification.
     * @return A map of tag names to a list of operations under that tag.
     */
    public Map<String, List<OperationInfo>> getGroupedApiOperations(String apiId) {
        Map<String, List<OperationInfo>> grouped = new LinkedHashMap<>();
        Optional<ApiSpecification> apiSpecOptional = apiRegistry.getSpecification(apiId);

        if (apiSpecOptional.isEmpty()) {
            log.warn("Cannot get operations. OpenAPI spec not found for apiId: {}", apiId);
            return grouped;
        }
        OpenAPI openAPI = apiSpecOptional.get().openAPI();

        if (openAPI.getPaths() == null) return grouped;

        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                if (operation != null && operation.getOperationId() != null && !operation.getOperationId().isBlank()) {
                    String tag = (operation.getTags() != null && !operation.getTags().isEmpty())
                            ? operation.getTags().get(0)
                            : "Default";
                    String capitalizedTag = StringUtils.capitalize(tag);
                    OperationInfo info = new OperationInfo(operation.getOperationId(), operation.getSummary(), operation.getDescription(), capitalizedTag);
                    grouped.computeIfAbsent(capitalizedTag, k -> new ArrayList<>()).add(info);
                }
            });
        });
        return grouped;
    }

    public Optional<OperationDetails> findOperationDetailsById(OpenAPI openAPI, String operationId) {
        if (openAPI.getPaths() == null) return Optional.empty();
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet()) {
                if (opEntry.getValue() != null && operationId.equals(opEntry.getValue().getOperationId())) {
                    return Optional.of(new OperationDetails(pathEntry.getKey(), opEntry.getKey(), opEntry.getValue()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * REFACTORED: Generates a request body payload. It first tries to find a pre-defined example.
     * If none exists, it intelligently generates a payload from the schema itself.
     */
    private String getPayloadForOperation(OpenAPI openAPI, Operation operation) {
        if (operation == null || operation.getRequestBody() == null) {
            return "";
        }

        RequestBody requestBody = operation.getRequestBody();
        // Resolve $ref for RequestBody if it exists
        if (requestBody.get$ref() != null) {
            String ref = requestBody.get$ref().substring("#/components/requestBodies/".length());
            requestBody = openAPI.getComponents().getRequestBodies().get(ref);
        }

        if (requestBody == null || requestBody.getContent() == null || !requestBody.getContent().containsKey("application/json")) {
            return "";
        }

        MediaType mediaType = requestBody.getContent().get("application/json");

        // Priority 1: Use explicit examples from the spec
        if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            Example example = mediaType.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                return prettyPrintJson(example.getValue());
            }
        }

        // Priority 2: Generate a payload from the schema
        if (mediaType.getSchema() != null) {
            Map<String, Object> generatedExample = generateExampleFromSchema(mediaType.getSchema(), openAPI, new HashSet<>());
            if (!generatedExample.isEmpty()) {
                return prettyPrintJson(generatedExample);
            }
        }

        return "";
    }

    private String prettyPrintJson(Object object) {
        try {
            // FIX: Use the pretty printer for consistent, readable output.
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing example JSON", e);
            return "{\n  \"error\": \"Could not generate example payload.\"\n}";
        }
    }

    /**
     * Recursively generates an example Map from a given schema.
     *
     * @param schema The schema to generate an example for.
     * @param openAPI The full OpenAPI model for resolving references.
     * @param visitedRefs A set to track visited schemas and prevent circular reference loops.
     * @return A map representing the example JSON object.
     */
    private Map<String, Object> generateExampleFromSchema(Schema<?> schema, OpenAPI openAPI, Set<String> visitedRefs) {
        // Handle $ref and prevent circular loops
        if (schema.get$ref() != null) {
            if (visitedRefs.contains(schema.get$ref())) {
                return Map.of("...", "circular reference");
            }
            visitedRefs.add(schema.get$ref());
            String ref = schema.get$ref().substring("#/components/schemas/".length());
            schema = openAPI.getComponents().getSchemas().get(ref);
        }

        if (schema == null) {
            return Collections.emptyMap();
        }

        // If the schema itself has a top-level example, use it.
        if (schema.getExample() instanceof Map) {
            return (Map<String, Object>) schema.getExample();
        }

        Map<String, Object> exampleJson = new LinkedHashMap<>();
        Map<String, Schema> properties = schema.getProperties();

        if (properties != null) {
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Schema<?> propertySchema = entry.getValue();
                // Pass a new copy of visitedRefs for each property branch to allow non-circular reuse of schemas
                exampleJson.put(propertyName, generateExampleValueForProperty(propertySchema, openAPI, new HashSet<>(visitedRefs)));
            }
        }

        return exampleJson;
    }

    /**
     * Generates a single example value for a property schema.
     *
     * @param propertySchema The schema for the specific property.
     * @param openAPI The full OpenAPI model for resolving references.
     * @param visitedRefs A set to track visited schemas and prevent circular reference loops.
     * @return An example value (e.g., String, Integer, Map, List).
     */
    private Object generateExampleValueForProperty(Schema<?> propertySchema, OpenAPI openAPI, Set<String> visitedRefs) {
        // Priority 1: Use explicit example on the property
        if (propertySchema.getExample() != null) {
            return propertySchema.getExample();
        }

        // Priority 2: Use default value
        if (propertySchema.getDefault() != null) {
            return propertySchema.getDefault();
        }

        // Handle $ref and prevent circular loops
        if (propertySchema.get$ref() != null) {
            if (visitedRefs.contains(propertySchema.get$ref())) {
                return "circular reference";
            }
            visitedRefs.add(propertySchema.get$ref());
            String ref = propertySchema.get$ref().substring("#/components/schemas/".length());
            propertySchema = openAPI.getComponents().getSchemas().get(ref);
        }

        if (propertySchema == null) {
            return null;
        }

        // Priority 3: Generate based on type
        String type = propertySchema.getType();
        if (type == null) return null;

        return switch (type) {
            case "string" -> generateStringExample(propertySchema);
            case "integer" -> generateIntegerExample(propertySchema);
            case "number" -> generateNumberExample(propertySchema);
            case "boolean" -> true;
            case "object" -> generateExampleFromSchema(propertySchema, openAPI, visitedRefs);
            case "array" -> {
                Schema<?> itemsSchema = propertySchema.getItems();
                if (itemsSchema != null) {
                    // Generate an array with one example item
                    yield List.of(generateExampleValueForProperty(itemsSchema, openAPI, visitedRefs));
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
                default -> "string";
            };
        }
        return "string";
    }

    private Integer generateIntegerExample(Schema<?> propertySchema) {
        if (propertySchema.getMinimum() != null) {
            return propertySchema.getMinimum().intValue();
        }
        return 1000; // A reasonable default for amounts in cents
    }

    private Double generateNumberExample(Schema<?> propertySchema) {
        if (propertySchema.getMinimum() != null) {
            return propertySchema.getMinimum().doubleValue();
        }
        return 10.5;
    }

    // --- SNIPPET GENERATORS ---
    // (These remain private and are unchanged)

    private String generateCurlSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("curl -X " + method.toUpperCase() + " \\\n");
        snippet.append("  '").append(openAPI.getServers().get(0).getUrl()).append(path).append("' \\\n");
        snippet.append("  -H 'Content-Type: application/json' \\\n");
        snippet.append("  -H 'Authorization: Bearer sk_test_...'");
        if (!payload.isEmpty()) {
            snippet.append(" \\\n  -d '").append(payload).append("'");
        }
        return snippet.toString();
    }

    private String generatePythonSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toLowerCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("import requests\nimport json\n\n");
        snippet.append("url = '").append(openAPI.getServers().get(0).getUrl()).append(path).append("'\n");
        snippet.append("headers = {\n");
        snippet.append("    'Authorization': 'Bearer sk_test_...',\n");
        snippet.append("    'Content-Type': 'application/json'\n");
        snippet.append("}\n\n");
        if (!payload.isEmpty()) {
            snippet.append("payload = ").append(payload).append("\n\n");
            snippet.append("response = requests.").append(method).append("(url, headers=headers, data=json.dumps(payload))\n");
        } else {
            snippet.append("response = requests.").append(method).append("(url, headers=headers)\n");
        }
        snippet.append("\nprint(response.json())");
        return snippet.toString();
    }

    private String generateNodeJsSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toLowerCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("const axios = require('axios');\n\n");
        snippet.append("const url = '").append(openAPI.getServers().get(0).getUrl()).append(path).append("';\n");
        snippet.append("const options = {\n");
        snippet.append("  headers: {\n");
        snippet.append("    'Authorization': 'Bearer sk_test_...',\n");
        snippet.append("    'Content-Type': 'application/json'\n");
        snippet.append("  }\n");
        snippet.append("};\n\n");
        if (!payload.isEmpty()) {
            snippet.append("const data = ").append(payload).append(";\n\n");
            snippet.append("axios.").append(method).append("(url, data, options)\n");
        } else {
            snippet.append("axios.").append(method).append("(url, options)\n");
        }
        snippet.append("  .then(response => console.log(response.data))\n");
        snippet.append("  .catch(error => console.error('Error:', error.response ? error.response.data : error.message));");
        return snippet.toString();
    }

    private String generateJavaSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toUpperCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("import okhttp3.*;\n");
        snippet.append("import java.io.IOException;\n\n");
        snippet.append("public class ApiExample {\n");
        snippet.append("    public static void main(String[] args) throws IOException {\n");
        snippet.append("        OkHttpClient client = new OkHttpClient();\n\n");
        String bodyVariable = "null";
        if (!payload.isEmpty()) {
            snippet.append("        String json = \"\"\"").append(payload).append("\"\"\";\n");
            snippet.append("        RequestBody body = RequestBody.create(json, MediaType.get(\"application/json; charset=utf-8\"));\n");
            bodyVariable = "body";
        }
        snippet.append("        Request request = new Request.Builder()\n");
        snippet.append("            .url(\"").append(openAPI.getServers().get(0).getUrl()).append(path).append("\")\n");
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            snippet.append("            .").append(method.toLowerCase()).append("(").append(bodyVariable).append(")\n");
        } else {
            snippet.append("            .").append(method.toLowerCase()).append("()\n");
        }
        snippet.append("            .addHeader(\"Authorization\", \"Bearer sk_test_...\")\n");
        snippet.append("            .build();\n\n");
        snippet.append("        try (Response response = client.newCall(request).execute()) {\n");
        snippet.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        snippet.append("            System.out.println(response.body().string());\n");
        snippet.append("        }\n");
        snippet.append("    }\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String generateCSharpSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("using RestSharp;\n\n");
        snippet.append("var options = new RestClientOptions(\"").append(openAPI.getServers().get(0).getUrl()).append("\");\n");
        snippet.append("var client = new RestClient(options);\n\n");
        snippet.append("var request = new RestRequest(\"").append(path).append("\", Method.").append(StringUtils.capitalize(method.toLowerCase())).append(");\n");
        snippet.append("request.AddHeader(\"Authorization\", \"Bearer sk_test_...\");\n");
        if (!payload.isEmpty()) {
            snippet.append("request.AddHeader(\"Content-Type\", \"application/json\");\n");
            snippet.append("var body = @\"\"\"").append(payload).append("\"\"\";\n");
            snippet.append("request.AddStringBody(body, DataFormat.Json);\n");
        }
        snippet.append("\nRestResponse response = await client.ExecuteAsync(request);\n");
        snippet.append("Console.WriteLine(response.Content);");
        return snippet.toString();
    }

    private String generatePhpSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toUpperCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("<?php\n\n");
        snippet.append("$curl = curl_init();\n\n");
        snippet.append("curl_setopt_array($curl, [\n");
        snippet.append("  CURLOPT_URL => '").append(openAPI.getServers().get(0).getUrl()).append(path).append("',\n");
        snippet.append("  CURLOPT_RETURNTRANSFER => true,\n");
        snippet.append("  CURLOPT_CUSTOMREQUEST => '").append(method).append("',\n");
        snippet.append("  CURLOPT_HTTPHEADER => [\n");
        snippet.append("    'Authorization: Bearer sk_test_...',\n");
        snippet.append("    'Content-Type: application/json'\n");
        snippet.append("  ],\n");
        if (!payload.isEmpty()) {
            snippet.append("  CURLOPT_POSTFIELDS => '").append(payload).append("'\n");
        }
        snippet.append("]);\n\n");
        snippet.append("$response = curl_exec($curl);\n");
        snippet.append("$err = curl_error($curl);\n\n");
        snippet.append("curl_close($curl);\n\n");
        snippet.append("if ($err) {\n");
        snippet.append("  echo 'cURL Error #:' . $err;\n");
        snippet.append("} else {\n");
        snippet.append("  echo $response;\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String generateGoSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toUpperCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("package main\n\n");
        snippet.append("import (\n");
        snippet.append("    \"fmt\"\n");
        snippet.append("    \"io\"\n");
        snippet.append("    \"log\"\n");
        snippet.append("    \"net/http\"\n");
        if (!payload.isEmpty()) {
            snippet.append("    \"strings\"\n");
        }
        snippet.append(")\n\n");
        snippet.append("func main() {\n");
        snippet.append("    url := \"").append(openAPI.getServers().get(0).getUrl()).append(path).append("\"\n");
        snippet.append("    method := \"").append(method).append("\"\n\n");
        String payloadVar = "nil";
        if (!payload.isEmpty()) {
            snippet.append("    payload := strings.NewReader(`").append(payload).append("`)\n");
            payloadVar = "payload";
        }
        snippet.append("    client := &http.Client{}\n");
        snippet.append("    req, err := http.NewRequest(method, url, ").append(payloadVar).append(")\n\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    req.Header.Add(\"Authorization\", \"Bearer sk_test_...\")\n");
        if (!payload.isEmpty()) {
            snippet.append("    req.Header.Add(\"Content-Type\", \"application/json\")\n");
        }
        snippet.append("\n");
        snippet.append("    res, err := client.Do(req)\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    defer res.Body.Close()\n\n");
        snippet.append("    body, err := io.ReadAll(res.Body)\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    fmt.Println(string(body))\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String generateRubySnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("require 'uri'\nrequire 'net/http'\nrequire 'json'\n\n");
        snippet.append("url = URI(\"").append(openAPI.getServers().get(0).getUrl()).append(path).append("\")\n\n");
        snippet.append("http = Net::HTTP.new(url.host, url.port)\n");
        snippet.append("http.use_ssl = true\n\n");
        snippet.append("request = Net::HTTP::").append(StringUtils.capitalize(method.toLowerCase())).append(".new(url)\n");
        snippet.append("request['Authorization'] = 'Bearer sk_test_...'\n");
        if (!payload.isEmpty()) {
            snippet.append("request['Content-Type'] = 'application/json'\n");
            snippet.append("request.body = JSON.dump(").append(payload).append(")\n");
        }
        snippet.append("\nresponse = http.request(request)\n");
        snippet.append("puts response.read_body");
        return snippet.toString();
    }

    private String generateSwiftSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toUpperCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("import Foundation\n#if canImport(FoundationNetworking)\nimport FoundationNetworking\n#endif\n\n");
        snippet.append("let headers = [\n");
        snippet.append("    \"Authorization\": \"Bearer sk_test_...\",\n");
        snippet.append("    \"Content-Type\": \"application/json\"\n");
        snippet.append("]\n\n");
        String postDataVar = "nil";
        if (!payload.isEmpty()) {
            snippet.append("let postData = Data(\"\"\"\n").append(payload).append("\n\"\"\".utf8)\n\n");
            postDataVar = "postData";
        }
        snippet.append("var request = URLRequest(url: URL(string: \"").append(openAPI.getServers().get(0).getUrl()).append(path).append("\")!,\n");
        snippet.append("                                cachePolicy: .useProtocolCachePolicy,\n");
        snippet.append("                                timeoutInterval: 10.0)\n");
        snippet.append("request.httpMethod = \"").append(method).append("\"\n");
        snippet.append("request.allHTTPHeaderFields = headers\n");
        if (!"nil".equals(postDataVar)) {
            snippet.append("request.httpBody = ").append(postDataVar).append("\n");
        }
        snippet.append("\n");
        snippet.append("let session = URLSession.shared\n");
        snippet.append("let dataTask = session.dataTask(with: request, completionHandler: { (data, response, error) -> Void in\n");
        snippet.append("    if let error = error {\n");
        snippet.append("        print(error)\n");
        snippet.append("    } else if let httpResponse = response as? HTTPURLResponse, let data = data {\n");
        snippet.append("        print(\"HTTP Status: \\(httpResponse.statusCode)\")\n");
        snippet.append("        let dataString = String(data: data, encoding: .utf8)\n");
        snippet.append("        print(dataString ?? \"No data\")\n");
        snippet.append("    }\n");
        snippet.append("})\n\n");
        snippet.append("dataTask.resume()");
        return snippet.toString();
    }

    private String generateKotlinSnippet(OpenAPI openAPI, OperationDetails details, String payload) {
        String method = details.method().toString().toLowerCase();
        String path = details.path();
        StringBuilder snippet = new StringBuilder("import okhttp3.*\n");
        snippet.append("import okhttp3.MediaType.Companion.toMediaType\n");
        snippet.append("import okhttp3.RequestBody.Companion.toRequestBody\n");
        snippet.append("import java.io.IOException\n\n");
        snippet.append("fun main() {\n");
        snippet.append("    val client = OkHttpClient()\n\n");
        String bodyVariable = "null";
        if (!payload.isEmpty()) {
            snippet.append("    val json = \"\"\"").append(payload).append("\"\"\"\n");
            snippet.append("    val body = json.toRequestBody(\"application/json; charset=utf-8\".toMediaType())\n");
            bodyVariable = "body";
        }
        snippet.append("    val request = Request.Builder()\n");
        snippet.append("        .url(\"").append(openAPI.getServers().get(0).getUrl()).append(path).append("\")\n");
        if ("post".equals(method) || "put".equals(method) || "patch".equals(method)) {
            snippet.append("        .").append(method).append("(").append(bodyVariable).append(")\n");
        } else {
            snippet.append("        .").append(method).append("()\n");
        }
        snippet.append("        .addHeader(\"Authorization\", \"Bearer sk_test_...\")\n");
        snippet.append("        .build()\n\n");
        snippet.append("    client.newCall(request).execute().use { response ->\n");
        snippet.append("        if (!response.isSuccessful) throw IOException(\"Unexpected code $response\")\n");
        snippet.append("        println(response.body!!.string())\n");
        snippet.append("    }\n");
        snippet.append("}");
        return snippet.toString();
    }
}