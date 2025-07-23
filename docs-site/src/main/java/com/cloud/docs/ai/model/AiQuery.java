package com.cloud.docs.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for an AI assistant query.
 *
 * @param query The user's question.
 */
public record AiQuery(
    @NotBlank(message = "Query cannot be empty")
    // FIX: Lowered the minimum character count to be more user-friendly.
    @Size(min = 2, max = 500, message = "Query must be between 2 and 500 characters")
    String query
) {}