package com.cloud.docs.ai.model;

import java.util.List;

/**
 * Response DTO for an AI assistant query.
 *
 * @param answer The generated answer from the AI.
 * @param sources A list of document chunks used as context to generate the answer.
 */
public record AiResponse(String answer, List<DocumentChunk> sources) {}