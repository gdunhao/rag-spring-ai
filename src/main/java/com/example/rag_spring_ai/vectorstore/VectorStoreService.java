package com.example.rag_spring_ai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service demonstrating Vector Store operations.
 *
 * The vector store is the heart of RAG — it stores document embeddings and
 * enables semantic similarity search. This demo shows:
 *
 * 1. Adding documents manually
 * 2. Performing similarity search with different parameters
 * 3. Understanding similarity scores and top-K retrieval
 * 4. Inspecting embedding dimensions
 */
@Service
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public VectorStoreService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Add sample documents directly to the vector store.
     * Each Document has content, metadata, and a generated embedding.
     */
    public Map<String, Object> addSampleDocuments() {
        List<Document> documents = List.of(
                new Document("Java is a statically-typed, object-oriented programming language. It runs on the JVM and follows the 'write once, run anywhere' principle.",
                        Map.of("topic", "java", "type", "language")),
                new Document("Python is a dynamically-typed, interpreted language known for its simplicity and extensive library ecosystem. It's widely used in data science and AI.",
                        Map.of("topic", "python", "type", "language")),
                new Document("Spring Boot simplifies building production-ready Spring applications. It provides auto-configuration, embedded servers, and opinionated defaults.",
                        Map.of("topic", "spring-boot", "type", "framework")),
                new Document("Docker containers package applications with their dependencies into standardized units. Containers are lightweight, portable, and consistent across environments.",
                        Map.of("topic", "docker", "type", "tool")),
                new Document("Kubernetes orchestrates containerized applications across clusters. It handles scaling, load balancing, rolling updates, and self-healing.",
                        Map.of("topic", "kubernetes", "type", "tool")),
                new Document("Machine learning models learn patterns from data to make predictions. Common types include supervised learning, unsupervised learning, and reinforcement learning.",
                        Map.of("topic", "ml", "type", "concept"))
        );

        vectorStore.add(documents);

        return Map.of(
                "documentsAdded", documents.size(),
                "status", "success"
        );
    }

    /**
     * Perform a similarity search and return results with metadata.
     *
     * @param query The search query
     * @param topK  Number of results to return
     */
    public List<Map<String, Object>> search(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()
        );

        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getText(),
                        "metadata", doc.getMetadata(),
                        "id", doc.getId()
                ))
                .toList();
    }

    /**
     * Search with a similarity threshold.
     * Only returns documents above the minimum similarity score.
     */
    public List<Map<String, Object>> searchWithThreshold(String query, double threshold) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)
                        .similarityThreshold(threshold)
                        .build()
        );

        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getText(),
                        "metadata", doc.getMetadata()
                ))
                .toList();
    }

    /**
     * Get embedding information — useful for understanding vector dimensions.
     */
    public Map<String, Object> getEmbeddingInfo(String text) {
        float[] embedding = embeddingModel.embed(text);
        return Map.of(
                "text", text,
                "dimensions", embedding.length,
                "sampleValues", List.of(embedding[0], embedding[1], embedding[2]),
                "model", "nomic-embed-text"
        );
    }
}

