package com.cloud.docs.ai.service;

import com.cloud.docs.ai.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * An in-memory vector store for DocumentChunks.
 * Provides functionality to add documents and search for the most similar ones.
 */
@Component
@Slf4j
public class VectorStore {

    private final List<DocumentChunk> documents = new CopyOnWriteArrayList<>();

    public void addDocument(DocumentChunk chunk) {
        documents.add(chunk);
    }

    public int size() {
        return documents.size();
    }

    public void clear() {
        documents.clear();
        log.info("VectorStore cleared.");
    }

    public List<DocumentChunk> search(List<Double> queryEmbedding, int k) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(chunk -> {
                    double similarity = cosineSimilarity(queryEmbedding, chunk.embedding());
                    return new SimilarityResult(chunk, similarity);
                })
                .sorted(Comparator.comparingDouble(SimilarityResult::similarity).reversed())
                .limit(k)
                .map(SimilarityResult::chunk)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size() || vectorA.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record SimilarityResult(DocumentChunk chunk, double similarity) {}
}