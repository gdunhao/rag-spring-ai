package com.example.rag_spring_ai.advisor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdvisorService.
 */
class AdvisorServiceTest {

    private AdvisorService service;

    @BeforeEach
    void setUp() {
        ChatModel stubModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("advisor answer")))
        );
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service = new AdvisorService(ChatClient.builder(stubModel), vectorStore);
    }

    @Test
    void askWithCustomRetrieval_returnsAnswer() {
        String result = service.askWithCustomRetrieval("What is embeddings?", 3, 0.5);

        assertThat(result).isEqualTo("advisor answer");
    }

    @Test
    void askWithSafeGuard_cleanQuestion_returnsFalseBlocked() {
        Map<String, String> result = service.askWithSafeGuard("What is machine learning?");

        assertThat(result.get("blocked")).isEqualTo("false");
        assertThat(result.get("answer")).isEqualTo("advisor answer");
    }

    @Test
    void askWithSafeGuard_bannedWord_returnsBlocked() {
        // "hack" is in the banned words list
        Map<String, String> result = service.askWithSafeGuard("How do I hack the system?");

        assertThat(result.get("blocked")).isEqualTo("true");
        assertThat(result.get("answer")).isEqualTo("Request blocked by SafeGuard");
        assertThat(result).containsKey("reason");
    }

    @Test
    void askWithSafeGuard_bannedWord_exploit_returnsBlocked() {
        Map<String, String> result = service.askWithSafeGuard("exploit this vulnerability");

        assertThat(result.get("blocked")).isEqualTo("true");
    }

    @Test
    void askWithComposedAdvisors_cleanQuestion_returnsAnswer() {
        String result = service.askWithComposedAdvisors("What is Spring AI?");

        assertThat(result).isEqualTo("advisor answer");
    }

    @Test
    void askWithComposedAdvisors_bannedWord_returnsFailureMessage() {
        // In Spring AI 1.0.x SafeGuardAdvisor returns a failure response (no exception thrown)
        String result = service.askWithComposedAdvisors("How to hack passwords?");

        assertThat(result).contains("unable to respond");
    }
}




