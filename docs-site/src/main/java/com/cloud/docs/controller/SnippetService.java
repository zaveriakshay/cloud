package com.cloud.docs.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SnippetService {

    private static final Logger log = LoggerFactory.getLogger(SnippetService.class);
    private OpenAPI openApi;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("static/api/openapi.yaml");
            // Reading the file content as a string is more reliable than using getPath()
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                String yamlContent = FileCopyUtils.copyToString(reader);
                this.openApi = new OpenAPIV3Parser().readContents(yamlContent).getOpenAPI();
            }
        } catch (Exception e) {
            log.error("Failed to load or parse OpenAPI specification.", e);
        }
    }

    public String generateSnippet(String operationId, String target, String client) {
        if (openApi == null || "get-started".equals(operationId)) {
            return "# Select an API endpoint to see an example request.";
        }

        for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();
            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet()) {
                Operation operation = opEntry.getValue();
                if (operation != null && operationId.equals(operation.getOperationId())) {
                    String path = entry.getKey();
                    String method = opEntry.getKey().toString();
                    return switch (target.toLowerCase()) {
                        case "shell" -> generateCurlSnippet(path, method, operation);
                        case "python" -> generatePythonSnippet(path, method, operation);
                        case "javascript" -> generateNodeJsSnippet(path, method, operation);
                        case "java" -> generateJavaSnippet(path, method, operation);
                        case "csharp" -> generateCSharpSnippet(path, method, operation);
                        case "php" -> generatePhpSnippet(path, method, operation);
                        case "go" -> generateGoSnippet(path, method, operation);
                        case "ruby" -> generateRubySnippet(path, method, operation);
                        case "swift" -> generateSwiftSnippet(path, method, operation);
                        case "kotlin" -> generateKotlinSnippet(path, method, operation);
                        default -> "# Snippet not available for this language.";
                    };
                }
            }
        }
        return "# Operation not found in OpenAPI spec.";
    }

    // --- DYNAMIC PAYLOAD HELPERS ---

    private Map<String, Object> buildExamplePayload(Operation operation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (operation.getRequestBody() != null &&
                operation.getRequestBody().getContent() != null &&
                operation.getRequestBody().getContent().containsKey("application/json")) {

            Schema<?> requestSchema = operation.getRequestBody().getContent().get("application/json").getSchema();
            if (requestSchema != null && requestSchema.getProperties() != null) {
                requestSchema.getProperties().forEach((key, value) -> {
                    Schema<?> propertySchema = (Schema<?>) value;
                    payload.put((String) key, getExampleValueForSchema(propertySchema));
                });
            }
        }
        return payload;
    }

    private Object getExampleValueForSchema(Schema<?> schema) {
        if (schema.getExample() != null) {
            return schema.getExample();
        }
        return switch (schema.getType()) {
            case "string" -> "string_example";
            case "integer" -> 1000;
            case "number" -> 123.45;
            case "boolean" -> true;
            default -> "value";
        };
    }

    // --- DYNAMIC SNIPPET GENERATORS ---

    private String generateCurlSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("curl -X " + method.toUpperCase() + " \\\n");
        snippet.append("  '").append(openApi.getServers().get(0).getUrl()).append(path).append("' \\\n");
        snippet.append("  -H 'Content-Type: application/json' \\\n");
        snippet.append("  -u sk_test_...:");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            if (!payload.isEmpty()) {
                String jsonPayload = payload.entrySet().stream()
                        .map(e -> {
                            String value = e.getValue().toString();
                            if (e.getValue() instanceof String) value = "\"" + value + "\"";
                            return "    \"" + e.getKey() + "\": " + value;
                        })
                        .collect(Collectors.joining(",\n"));
                snippet.append(" \\\n  -d '{\n").append(jsonPayload).append("\n  }'");
            }
        }
        return snippet.toString();
    }

    private String generatePythonSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("import requests\nimport json\n\n");
        snippet.append("url = '").append(openApi.getServers().get(0).getUrl()).append(path).append("'\n");
        snippet.append("auth = ('sk_test_...', '')\n");
        snippet.append("headers = {'Content-Type': 'application/json'}\n\n");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String payloadStr = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "'" + value + "'";
                        return "    '" + e.getKey() + "': " + value;
                    })
                    .collect(Collectors.joining(",\n"));
            snippet.append("payload = {\n").append(payloadStr).append("\n}\n\n");
            snippet.append("response = requests.").append(method.toLowerCase()).append("(url, auth=auth, headers=headers, data=json.dumps(payload))\n");
        } else {
            snippet.append("response = requests.").append(method.toLowerCase()).append("(url, auth=auth, headers=headers)\n");
        }
        snippet.append("print(response.json())");
        return snippet.toString();
    }

    private String generateNodeJsSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("const axios = require('axios');\n\n");
        snippet.append("const url = '").append(openApi.getServers().get(0).getUrl()).append(path).append("';\n");
        snippet.append("const options = {\n");
        snippet.append("  auth: {\n");
        snippet.append("    username: 'sk_test_...',\n");
        snippet.append("    password: ''\n");
        snippet.append("  },\n");
        snippet.append("  headers: {'Content-Type': 'application/json'}\n");
        snippet.append("};\n\n");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String payloadStr = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "'" + value + "'";
                        return "  " + e.getKey() + ": " + value;
                    })
                    .collect(Collectors.joining(",\n"));
            snippet.append("const data = {\n").append(payloadStr).append("\n};\n\n");
            snippet.append("axios.").append(method.toLowerCase()).append("(url, data, options)\n");
        } else {
            snippet.append("axios.").append(method.toLowerCase()).append("(url, options)\n");
        }
        snippet.append("  .then(response => console.log(response.data))\n");
        snippet.append("  .catch(error => console.error('Error:', error.response ? error.response.data : error.message));");
        return snippet.toString();
    }

    private String generateJavaSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("import okhttp3.*;\n");
        snippet.append("import java.io.IOException;\n\n");
        snippet.append("public class ApiExample {\n");
        snippet.append("    public static void main(String[] args) throws IOException {\n");
        snippet.append("        OkHttpClient client = new OkHttpClient();\n\n");

        String bodyVariable = "null";
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String jsonPayload = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\\\"" + value.replace("\"", "\\\"") + "\\\"";
                        return "\\\"" + e.getKey() + "\\\":" + value;
                    })
                    .collect(Collectors.joining(","));
            snippet.append("        String json = \"{" + jsonPayload + "}\";\n");
            snippet.append("        RequestBody body = RequestBody.create(json, MediaType.get(\"application/json; charset=utf-8\"));\n");
            bodyVariable = "body";
        }

        snippet.append("        Request request = new Request.Builder()\n");
        snippet.append("            .url(\"").append(openApi.getServers().get(0).getUrl()).append(path).append("\")\n");
        if (!"GET".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            snippet.append("            .").append(method.toLowerCase()).append("(").append(bodyVariable).append(")\n");
        } else {
            snippet.append("            .").append(method.toLowerCase()).append("()\n");
        }
        snippet.append("            .addHeader(\"Authorization\", Credentials.basic(\"sk_test_...\", \"\"))\n");
        snippet.append("            .build();\n\n");
        snippet.append("        try (Response response = client.newCall(request).execute()) {\n");
        snippet.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        snippet.append("            System.out.println(response.body().string());\n");
        snippet.append("        }\n");
        snippet.append("    }\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String generateCSharpSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("using RestSharp;\n");
        snippet.append("using RestSharp.Authenticators;\n\n");
        snippet.append("var options = new RestClientOptions(\"").append(openApi.getServers().get(0).getUrl()).append("\")\n");
        snippet.append("{\n");
        snippet.append("    Authenticator = new HttpBasicAuthenticator(\"sk_test_...\", \"\")\n");
        snippet.append("};\n");
        snippet.append("var client = new RestClient(options);\n\n");
        snippet.append("var request = new RestRequest(\"").append(path).append("\", Method.").append(method).append(");\n");
        snippet.append("request.AddHeader(\"Content-Type\", \"application/json\");\n\n");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String jsonPayload = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\\\"" + value.replace("\"", "\\\"") + "\\\"";
                        return "\\\"" + e.getKey() + "\\\":" + value;
                    })
                    .collect(Collectors.joining(","));
            snippet.append("var body = @\"{" + jsonPayload + "}\";\n");
            snippet.append("request.AddParameter(\"application/json\", body, ParameterType.RequestBody);\n\n");
        }

        snippet.append("RestResponse response = client.Execute(request);\n");
        snippet.append("Console.WriteLine(response.Content);");
        return snippet.toString();
    }

    private String generatePhpSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("<?php\n\n");
        snippet.append("$curl = curl_init();\n\n");
        snippet.append("curl_setopt_array($curl, [\n");
        snippet.append("  CURLOPT_URL => '").append(openApi.getServers().get(0).getUrl()).append(path).append("',\n");
        snippet.append("  CURLOPT_RETURNTRANSFER => true,\n");
        snippet.append("  CURLOPT_CUSTOMREQUEST => '").append(method.toUpperCase()).append("',\n");
        snippet.append("  CURLOPT_USERPWD => 'sk_test_...:',\n");
        snippet.append("  CURLOPT_HTTPHEADER => [\n");
        snippet.append("    'Content-Type: application/json'\n");
        snippet.append("  ],\n");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String jsonPayload = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\\\"" + value.replace("\"", "\\\"") + "\\\"";
                        return "\\\"" + e.getKey() + "\\\":" + value;
                    })
                    .collect(Collectors.joining(","));
            snippet.append("  CURLOPT_POSTFIELDS => '{" + jsonPayload + "}',\n");
        }

        snippet.append("]);\n\n");
        snippet.append("$response = curl_exec($curl);\n");
        snippet.append("curl_close($curl);\n");
        snippet.append("echo $response;");
        return snippet.toString();
    }

    private String generateGoSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("package main\n\n");
        snippet.append("import (\n");
        snippet.append("    \"fmt\"\n");
        snippet.append("    \"io/ioutil\"\n");
        snippet.append("    \"log\"\n");
        snippet.append("    \"net/http\"\n");
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            snippet.append("    \"strings\"\n");
        }
        snippet.append(")\n\n");
        snippet.append("func main() {\n");
        snippet.append("    url := \"").append(openApi.getServers().get(0).getUrl()).append(path).append("\"\n");
        snippet.append("    method := \"").append(method.toUpperCase()).append("\"\n\n");

        String payloadVar = "nil";
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String jsonPayload = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\\\"" + value.replace("\"", "\\\"") + "\\\"";
                        return "\\\"" + e.getKey() + "\\\":" + value;
                    })
                    .collect(Collectors.joining(","));
            snippet.append("    payload := strings.NewReader(`{" + jsonPayload + "}`)\n");
            payloadVar = "payload";
        }

        snippet.append("    client := &http.Client{}\n");
        snippet.append("    req, err := http.NewRequest(method, url, ").append(payloadVar).append(")\n\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    req.SetBasicAuth(\"sk_test_...\", \"\")\n");
        snippet.append("    req.Header.Add(\"Content-Type\", \"application/json\")\n\n");
        snippet.append("    res, err := client.Do(req)\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    defer res.Body.Close()\n\n");
        snippet.append("    body, err := ioutil.ReadAll(res.Body)\n");
        snippet.append("    if err != nil {\n        log.Fatal(err)\n    }\n");
        snippet.append("    fmt.Println(string(body))\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String generateRubySnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("require 'uri'\nrequire 'net/http'\nrequire 'json'\n\n");
        snippet.append("url = URI(\"").append(openApi.getServers().get(0).getUrl()).append(path).append("\")\n\n");
        snippet.append("http = Net::HTTP.new(url.host, url.port)\n");
        snippet.append("http.use_ssl = true\n\n");
        snippet.append("request = Net::HTTP::").append(capitalize(method)).append(".new(url)\n");
        snippet.append("request['Content-Type'] = 'application/json'\n");
        snippet.append("request.basic_auth 'sk_test_...', ''\n\n");

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String payloadStr = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "'" + value + "'";
                        return "  '" + e.getKey() + "' => " + value;
                    })
                    .collect(Collectors.joining(",\n"));
            snippet.append("request.body = JSON.dump({\n").append(payloadStr).append("\n})\n\n");
        }

        snippet.append("response = http.request(request)\n");
        snippet.append("puts response.read_body");
        return snippet.toString();
    }

    private String generateSwiftSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("import Foundation\n\n");
        snippet.append("let headers = [\n");
        snippet.append("    \"Content-Type\": \"application/json\",\n");
        snippet.append("    \"Authorization\": \"Basic \" + \"sk_test_...:\".data(using: .utf8)!.base64EncodedString()\n");
        snippet.append("]\n\n");

        String postDataVar = "nil";
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String payloadStr = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\"" + value + "\"";
                        return "\"" + e.getKey() + "\": " + value;
                    })
                    .collect(Collectors.joining(", "));
            snippet.append("let postData = try? JSONSerialization.data(withJSONObject: [" + payloadStr + "], options: [])\n\n");
            postDataVar = "postData";
        }

        snippet.append("var request = URLRequest(url: URL(string: \"").append(openApi.getServers().get(0).getUrl()).append(path).append("\")!,\n");
        snippet.append("                                cachePolicy: .useProtocolCachePolicy,\n");
        snippet.append("                                timeoutInterval: 10.0)\n");
        snippet.append("request.httpMethod = \"").append(method.toUpperCase()).append("\"\n");
        snippet.append("request.allHTTPHeaderFields = headers\n");
        if (!"nil".equals(postDataVar)) {
            snippet.append("request.httpBody = ").append(postDataVar).append("\n");
        }
        snippet.append("\n");
        snippet.append("let session = URLSession.shared\n");
        snippet.append("let dataTask = session.dataTask(with: request, completionHandler: { (data, response, error) -> Void in\n");
        snippet.append("    if let error = error {\n");
        snippet.append("        print(error)\n");
        snippet.append("    } else if let data = data {\n");
        snippet.append("        let dataString = String(data: data, encoding: .utf8)\n");
        snippet.append("        print(dataString ?? \"No data\")\n");
        snippet.append("    }\n");
        snippet.append("})\n\n");
        snippet.append("dataTask.resume()");
        return snippet.toString();
    }

    private String generateKotlinSnippet(String path, String method, Operation operation) {
        StringBuilder snippet = new StringBuilder("import okhttp3.*\n");
        snippet.append("import okhttp3.MediaType.Companion.toMediaType\n");
        snippet.append("import okhttp3.RequestBody.Companion.toRequestBody\n");
        snippet.append("import java.io.IOException\n\n");
        snippet.append("fun main() {\n");
        snippet.append("    val client = OkHttpClient()\n\n");

        String bodyVariable = "null";
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && operation.getRequestBody() != null) {
            Map<String, Object> payload = buildExamplePayload(operation);
            String jsonPayload = payload.entrySet().stream()
                    .map(e -> {
                        String value = e.getValue().toString();
                        if (e.getValue() instanceof String) value = "\\\"" + value.replace("\"", "\\\"") + "\\\"";
                        return "\\\"" + e.getKey() + "\\\":" + value;
                    })
                    .collect(Collectors.joining(","));
            snippet.append("    val json = \"\"\"{" + jsonPayload + "}\"\"\"\n");
            snippet.append("    val body = json.toRequestBody(\"application/json; charset=utf-8\".toMediaType())\n");
            bodyVariable = "body";
        }

        snippet.append("    val request = Request.Builder()\n");
        snippet.append("        .url(\"").append(openApi.getServers().get(0).getUrl()).append(path).append("\")\n");
        if (!"GET".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            snippet.append("        .").append(method.toLowerCase()).append("(").append(bodyVariable).append(")\n");
        } else {
            snippet.append("        .").append(method.toLowerCase()).append("()\n");
        }
        snippet.append("        .addHeader(\"Authorization\", Credentials.basic(\"sk_test_...\", \"\"))\n");
        snippet.append("        .build()\n\n");
        snippet.append("    client.newCall(request).execute().use { response ->\n");
        snippet.append("        if (!response.isSuccessful) throw IOException(\"Unexpected code $response\")\n");
        snippet.append("        println(response.body!!.string())\n");
        snippet.append("    }\n");
        snippet.append("}");
        return snippet.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * NEW: Extracts API operations from the OpenAPI specification to dynamically build the navigation.
     *
     * @return A map where the key is the operationId and the value is the operation's summary.
     */
    public Map<String, String> getApiOperations() {
        if (openApi == null) {
            log.warn("OpenAPI spec is not loaded, cannot provide API operations.");
            return java.util.Collections.emptyMap();
        }
        // Use LinkedHashMap to preserve the order from the OpenAPI file
        Map<String, String> operations = new java.util.LinkedHashMap<>();

        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (operation != null && operation.getOperationId() != null && !operation.getOperationId().isBlank() && operation.getSummary() != null) {
                        operations.put(operation.getOperationId(), operation.getSummary());
                    }
                })
        );
        return operations;
    }
}