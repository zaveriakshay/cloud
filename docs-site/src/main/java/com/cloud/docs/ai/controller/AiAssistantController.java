package com.cloud.docs.ai.controller;

import com.cloud.docs.ai.model.AiQuery;
import com.cloud.docs.ai.model.AiResponse;
import com.cloud.docs.ai.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the AI Assistant backend.
 * Handles incoming user queries and returns AI-generated responses.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "Endpoints for interacting with the documentation AI assistant.")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @Operation(summary = "Ask a question to the AI documentation assistant")
    @PostMapping("/ask")
    public ResponseEntity<AiResponse> askQuestion(@Valid @RequestBody AiQuery aiQuery) {
        log.debug("Received AI query from frontend: {}", aiQuery.query());
        AiResponse response = aiAssistantService.askQuestion(aiQuery.query());
        return ResponseEntity.ok(response);
    }
}