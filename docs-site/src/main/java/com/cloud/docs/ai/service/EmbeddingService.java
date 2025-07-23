package com.cloud.docs.ai.service;

import com.cloud.docs.ai.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Service responsible for converting text into numerical embeddings and chunking documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    public List<Double> embed(String text) {
        // Per your request, this code handles the conversion from a float[] to a List<Double>.
        // It assumes the underlying model call returns a float array, as indicated by the compilation error.
        float[] embedArray = this.embeddingModel.embed(text);

        if (embedArray == null || embedArray.length == 0) {
            log.error("Failed to get embedding for text. The response was empty.");
            throw new RuntimeException("Embedding failed. The AI model returned an empty response.");
        }

        // Manually convert the float array to a List of Doubles.
        List<Double> embedding = new ArrayList<>(embedArray.length);
        for (float value : embedArray) {
            embedding.add((double) value);
        }

        return embedding;
    }

    public List<DocumentChunk> chunkDocument(String documentContent, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (documentContent == null || documentContent.isBlank()) {
            return chunks;
        }

        String cleanedContent = documentContent.replaceAll("\\s+", " ").trim();

        int currentPosition = 0;
        while (currentPosition < cleanedContent.length()) {
            int endPosition = Math.min(currentPosition + CHUNK_SIZE, cleanedContent.length());
            String chunkContent = cleanedContent.substring(currentPosition, endPosition);

            String chunkId = generateChunkId(chunkContent, metadata.get("url"));
            List<Double> embedding = embed(chunkContent);

            // This now compiles correctly because DocumentChunk expects a List<Double>
            chunks.add(new DocumentChunk(chunkId, chunkContent, embedding, metadata));

            if (endPosition == cleanedContent.length()) {
                break;
            }

            currentPosition += (CHUNK_SIZE - CHUNK_OVERLAP);
        }
        log.debug("Chunked document into {} chunks. First chunk: {}", chunks.size(), chunks.isEmpty() ? "N/A" : chunks.get(0).id());
        return chunks;
    }

    private String generateChunkId(String content, String sourceUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String uniqueString = content + (sourceUrl != null ? sourceUrl : "");
            byte[] hash = digest.digest(uniqueString.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found. Falling back to hashCode for chunk ID.", e);
            return String.valueOf((content + sourceUrl).hashCode());
        }
    }
}