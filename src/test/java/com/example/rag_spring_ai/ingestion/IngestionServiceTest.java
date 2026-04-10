package com.example.rag_spring_ai.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IngestionService.
 */
class IngestionServiceTest {

    private VectorStore vectorStore;
    private IngestionService service;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        service = new IngestionService(
                vectorStore,
                new ClassPathResource("documents/sample/spring-ai-overview.txt"),
                new ClassPathResource("documents/sample/ai-concepts.json")
        );
    }

    @Test
    void ingestText_addsDocumentsToVectorStore() {
        Map<String, Object> result = service.ingestText();

        verify(vectorStore, times(1)).add(anyList());
        assertThat(result.get("status")).isEqualTo("ingested");
        assertThat(result.get("source")).isEqualTo("spring-ai-overview.txt");
        assertThat(result.get("format")).isEqualTo("text");
    }

    @Test
    void ingestText_returnsNonZeroChunkCount() {
        Map<String, Object> result = service.ingestText();

        int chunks = (int) result.get("chunksCreated");
        assertThat(chunks).isGreaterThan(0);
    }

    @Test
    void ingestJson_addsDocumentsToVectorStore() {
        Map<String, Object> result = service.ingestJson();

        verify(vectorStore, times(1)).add(anyList());
        assertThat(result.get("status")).isEqualTo("ingested");
        assertThat(result.get("source")).isEqualTo("ai-concepts.json");
        assertThat(result.get("format")).isEqualTo("json");
    }

    @Test
    void ingestWithCustomChunking_addsChunksAndReturnsChunkSize() {
        Map<String, Object> result = service.ingestWithCustomChunking(400, 50);

        verify(vectorStore, times(1)).add(anyList());
        assertThat(result.get("status")).isEqualTo("ingested");
        assertThat(result.get("chunkSize")).isEqualTo(400);
        assertThat(result.get("minChunkSize")).isEqualTo(50);
    }

    @Test
    void ingestWithCustomChunking_smallerChunks_producesMoreChunks() {
        Map<String, Object> largeChunks = service.ingestWithCustomChunking(800, 100);
        Map<String, Object> smallChunks = service.ingestWithCustomChunking(300, 50);

        // Smaller chunk size should result in at least as many chunks
        int largeCnt = (int) largeChunks.get("chunksCreated");
        int smallCnt = (int) smallChunks.get("chunksCreated");
        assertThat(smallCnt).isGreaterThanOrEqualTo(largeCnt);
    }
}

