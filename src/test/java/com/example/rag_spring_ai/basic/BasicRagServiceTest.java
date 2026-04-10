package com.example.rag_spring_ai.basic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for BasicRagService.
 *
 * Uses a real ChatClient backed by a stub ChatModel + a mocked VectorStore.
 * No Spring context required.
 */
class BasicRagServiceTest {

    private VectorStore vectorStore;
    private BasicRagService service;

    @BeforeEach
    void setUp() {
        ChatModel stubModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("Spring AI is a framework for AI applications.")))
        );
        vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service = new BasicRagService(
                ChatClient.builder(stubModel),
                vectorStore,
                new ClassPathResource("documents/sample/spring-ai-overview.txt")
        );
    }

    @Test
    void ingestDocuments_addsChunksToVectorStore() {
        service.ingestDocuments();

        verify(vectorStore, atLeastOnce()).add(anyList());
    }

    @Test
    void ingestDocuments_calledTwice_onlyIngestsOnce() {
        service.ingestDocuments();
        service.ingestDocuments();

        // AtomicBoolean guard should prevent double ingestion
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void ask_triggersIngestionAndReturnsAnswer() {
        String answer = service.ask("What is Spring AI?");

        // Ingestion must have happened
        verify(vectorStore, times(1)).add(anyList());
        // Answer comes back from the stub model
        assertThat(answer).isEqualTo("Spring AI is a framework for AI applications.");
    }

    @Test
    void ask_calledTwice_doesNotIngestAgain() {
        service.ask("First question");
        service.ask("Second question");

        verify(vectorStore, times(1)).add(anyList());
    }
}



