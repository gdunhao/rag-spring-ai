package com.example.rag_spring_ai.memory;

import com.example.rag_spring_ai.config.StubEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatMemoryService session-lifecycle logic.
 *
 * Uses a real ChatClient backed by a stub ChatModel so no Spring context is required.
 */
class ChatMemoryServiceTest {

    private ChatMemoryService service;

    @BeforeEach
    void setUp() {
        ChatModel stubModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("stub answer")))
        );
        VectorStore vectorStore = SimpleVectorStore.builder(new StubEmbeddingModel()).build();
        service = new ChatMemoryService(ChatClient.builder(stubModel), vectorStore);
    }

    @Test
    void getSessionInfo_initially_hasNoActiveSessions() {
        Map<String, Object> info = service.getSessionInfo();

        assertThat(info.get("activeSessions")).isEqualTo(0);
    }

    @Test
    void chat_addsSessionToActiveSessions() {
        service.chat("session-1", "Hello");

        Map<String, Object> info = service.getSessionInfo();
        assertThat(info.get("activeSessions")).isEqualTo(1);
        assertThat(info.get("sessionIds")).asList().contains("session-1");
    }

    @Test
    void chatWithoutRag_addsSessionToActiveSessions() {
        service.chatWithoutRag("session-2", "Hi there");

        Map<String, Object> info = service.getSessionInfo();
        assertThat(info.get("sessionIds")).asList().contains("session-2");
    }

    @Test
    void clearSession_removesSessionFromActiveSessions() {
        service.chat("session-A", "message");
        assertThat((int) service.getSessionInfo().get("activeSessions")).isEqualTo(1);

        service.clearSession("session-A");

        assertThat((int) service.getSessionInfo().get("activeSessions")).isEqualTo(0);
    }

    @Test
    void multipleSessions_areTrackedIndependently() {
        service.chat("s1", "m1");
        service.chat("s2", "m2");
        service.chat("s3", "m3");

        assertThat((int) service.getSessionInfo().get("activeSessions")).isEqualTo(3);

        service.clearSession("s2");

        assertThat((int) service.getSessionInfo().get("activeSessions")).isEqualTo(2);
        assertThat(service.getSessionInfo().get("sessionIds")).asList()
                .containsExactlyInAnyOrder("s1", "s3");
    }

    @Test
    void clearSession_nonExistentSession_doesNotThrow() {
        // Should not throw — clearing a session that was never created is a no-op
        service.clearSession("ghost-session");

        assertThat((int) service.getSessionInfo().get("activeSessions")).isEqualTo(0);
    }
}



