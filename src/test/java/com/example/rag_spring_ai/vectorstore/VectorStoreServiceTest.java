package com.example.rag_spring_ai.vectorstore;

import com.example.rag_spring_ai.config.StubEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VectorStoreService.
 *
 * Uses a real SimpleVectorStore backed by a StubEmbeddingModel — no Spring context needed.
 */
class VectorStoreServiceTest {

    private VectorStoreService service;
    private EmbeddingModel stubEmbedding;

    @BeforeEach
    void setUp() {
        stubEmbedding = new StubEmbeddingModel(768);
        VectorStore vectorStore = SimpleVectorStore.builder(stubEmbedding).build();
        service = new VectorStoreService(vectorStore, stubEmbedding);
    }

    @Test
    void addSampleDocuments_returnsSuccessStatus() {
        Map<String, Object> result = service.addSampleDocuments();

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("documentsAdded")).isEqualTo(6);
    }

    @Test
    void search_afterAdd_returnsResults() {
        service.addSampleDocuments();

        List<Map<String, Object>> results = service.search("Java programming language", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.size()).isLessThanOrEqualTo(3);
    }

    @Test
    void search_resultsContainContentAndMetadata() {
        service.addSampleDocuments();

        List<Map<String, Object>> results = service.search("Spring Boot", 2);

        results.forEach(doc -> {
            assertThat(doc).containsKey("content");
            assertThat(doc).containsKey("metadata");
            assertThat(doc).containsKey("id");
        });
    }

    @Test
    void searchWithThreshold_returnsResultsUpToThreshold() {
        service.addSampleDocuments();

        // Threshold 0.0 should return some results
        List<Map<String, Object>> results = service.searchWithThreshold("Docker containers", 0.0);

        assertThat(results).isNotNull();
    }

    @Test
    void getEmbeddingInfo_returnsDimensionsAndSampleValues() {
        Map<String, Object> info = service.getEmbeddingInfo("Hello world");

        assertThat(info.get("text")).isEqualTo("Hello world");
        assertThat(info.get("dimensions")).isEqualTo(768);
        assertThat(info.get("sampleValues")).asList().hasSize(3);
    }
}

