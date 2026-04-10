package com.example.rag_spring_ai.metadata;

import com.example.rag_spring_ai.config.StubEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MetadataFilterService.
 *
 * Uses a real SimpleVectorStore with a stub EmbeddingModel so documents are
 * actually stored and searched in memory (no Spring context, no PostgreSQL).
 */
class MetadataFilterServiceTest {

    private MetadataFilterService service;
    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        ChatModel stubModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("product answer")))
        );
        vectorStore = SimpleVectorStore.builder(new StubEmbeddingModel()).build();
        service = new MetadataFilterService(ChatClient.builder(stubModel), vectorStore);
        // Run @PostConstruct manually in unit tests
        service.ingestDocumentsWithMetadata();
    }

    @Test
    void ingestDocumentsWithMetadata_populatesVectorStore() {
        // After @PostConstruct the store should have documents; verify by searching without filter
        List<Document> results = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query("CloudFlow")
                        .topK(10)
                        .build()
        );
        assertThat(results).isNotEmpty();
    }

    @Test
    void searchByProduct_returnsOnlyMatchingProduct() {
        List<Map<String, Object>> results = service.searchByProduct("version features", "cloudflow");

        // All results should be from cloudflow
        results.forEach(r -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) r.get("metadata");
            assertThat(meta.get("product")).isEqualTo("cloudflow");
        });
    }

    @Test
    void searchByProduct_datasync_returnsOnlyDatasyncDocs() {
        List<Map<String, Object>> results = service.searchByProduct("synchronization", "datasync");

        results.forEach(r -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) r.get("metadata");
            assertThat(meta.get("product")).isEqualTo("datasync");
        });
    }

    @Test
    void searchByCategory_returnsOnlyMatchingCategory() {
        List<Map<String, Object>> results = service.searchByCategory("release", "release-notes");

        results.forEach(r -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) r.get("metadata");
            assertThat(meta.get("category")).isEqualTo("release-notes");
        });
    }

    @Test
    void askAboutProduct_returnsAnswer() {
        String answer = service.askAboutProduct("What API rate limits exist?", "cloudflow");

        assertThat(answer).isEqualTo("product answer");
    }
}



