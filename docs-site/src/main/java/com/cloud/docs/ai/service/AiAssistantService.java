package com.cloud.docs.ai.service;

import com.cloud.docs.ai.model.AiResponse;
import com.cloud.docs.ai.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for the AI Assistant. Handles the RAG (Retrieval-Augmented Generation) flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    private static final int NUM_CONTEXT_CHUNKS = 5;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an expert AI assistant for the noqodiDocs documentation.
            Your task is to answer the user's question based *only* on the provided context documentation below.
            Do not use any prior knowledge. If the answer is not in the context, say "I'm sorry, but I couldn't find information about that in the documentation."
            Be concise and helpful.
            """;

    public AiResponse askQuestion(String userQuery) {
        log.info("Received AI query: {}", userQuery);

        // 1. Embed the user's query to get a List<Double>
        List<Double> queryEmbedding = embeddingService.embed(userQuery);

        // 2. Retrieve relevant document chunks from the vector store (no conversion needed)
        List<DocumentChunk> relevantChunks = vectorStore.search(queryEmbedding, NUM_CONTEXT_CHUNKS);

        if (relevantChunks.isEmpty()) {
            log.warn("No relevant chunks found for query: {}", userQuery);
            return new AiResponse("I'm sorry, but I couldn't find information about that in the documentation.", List.of());
        }

        // 3. Construct the prompt for the LLM
        String context = relevantChunks.stream()
                .map(DocumentChunk::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        ChatClient chatClient = chatClientBuilder.build();

        String llmAnswer = chatClient.prompt()
                .system(SYSTEM_PROMPT_TEMPLATE)
                .user(user -> user
                        .text("CONTEXT: \n{context}\n\nUSER QUESTION: {query}")
                        .param("context", context)
                        .param("query", userQuery)
                )
                .call()
                .content();

        log.info("Generated AI answer for query: '{}'", userQuery);
        return new AiResponse(llmAnswer, relevantChunks);
    }
}