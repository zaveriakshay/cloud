package com.cloud.docs.ai.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a chunk of text from the documentation, along with its embedding and metadata.
 * This is the unit stored in the VectorStore.
 *
 * @param id A unique identifier for the chunk (e.g., hash of content + source).
 * @param content The actual text content of the chunk.
 * @param embedding The numerical vector representation of the content.
 * @param metadata Additional information about the chunk's origin (e.g., title, URL, breadcrumb).
 */
public record DocumentChunk(String id, String content, List<Double> embedding, Map<String, String> metadata) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentChunk that = (DocumentChunk) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
               "id='" + id + '\'' +
               ", content='" + content.substring(0, Math.min(content.length(), 50)) + "...'" +
               ", embedding=" + (embedding != null ? "List(size=" + embedding.size() + ")" : "null") +
               ", metadata=" + metadata +
               '}';
    }
}