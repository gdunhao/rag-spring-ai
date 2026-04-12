package com.example.rag_spring_ai.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service demonstrating conversational RAG with chat memory.
 *
 * Chat memory allows the LLM to maintain context across multiple turns in a
 * conversation. Combined with RAG, this creates a powerful conversational
 * assistant that can:
 *
 * 1. Remember previous questions and answers in the session
 * 2. Resolve pronouns and references (e.g., "tell me more about that")
 * 3. Build on previous context for follow-up questions
 *
 * Each session is identified by a session ID passed as an advisor parameter at
 * call time — no per-request ChatClient is built.
 *
 * Spring AI 1.0 pattern:
 *  - One shared {@link InMemoryChatMemoryRepository} backs all conversations.
 *  - One {@link MessageWindowChatMemory} wraps it as the memory policy layer.
 *  - The session ID is injected via {@link ChatMemory#CONVERSATION_ID} as an
 *    advisor param so the advisor reads/writes the correct conversation slice.
 */
@Service
public class ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    // Tracks which sessions are active; used only for informational purposes.
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public ChatMemoryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .build();

        // Build once — MessageChatMemoryAdvisor is a default; RAG advisor added per-call
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a helpful conversational assistant with access to a knowledge base.
                        Use the retrieved context to answer questions. Remember the conversation
                        history and use it to understand follow-up questions.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
        log.info("[Memory] ChatMemoryService initialised | store=InMemoryChatMemoryRepository");
    }

    /**
     * Chat with memory and RAG — each session maintains conversation history.
     * The QuestionAnswerAdvisor is added per-call on top of the default memory advisor.
     */
    public String chat(String sessionId, String message) {
        boolean isNew = activeSessions.add(sessionId);
        log.info("[Memory]    Session {} | message='{}'", isNew ? "NEW" : "RESUMED", message);
        log.info("[→VectorDB] Similarity search via QuestionAnswerAdvisor | sessionId={}", sessionId);
        log.info("[→Ollama]   Chat request (RAG+Memory) | model=qwen3:4b | sessionId={}", sessionId);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(message)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms | sessionId={}",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0, sessionId);
        return response;
    }

    /**
     * Chat without RAG — pure conversational memory demo.
     * Shows memory working independently of document retrieval.
     */
    public String chatWithoutRag(String sessionId, String message) {
        boolean isNew = activeSessions.add(sessionId);
        log.info("[Memory]    Session {} (memory-only, no RAG) | message='{}'", isNew ? "NEW" : "RESUMED", message);
        log.info("[→Ollama]   Chat request (Memory only) | model=qwen3:4b | sessionId={}", sessionId);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .system("You are a friendly assistant. Remember our conversation history.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(message)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms | sessionId={}",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0, sessionId);
        return response;
    }

    /**
     * Clear a session's memory using the shared ChatMemory's clear method.
     */
    public void clearSession(String sessionId) {
        chatMemory.clear(sessionId);
        activeSessions.remove(sessionId);
        log.info("[Memory]    Session CLEARED | sessionId={}", sessionId);
    }

    /**
     * List active sessions.
     */
    public Map<String, Object> getSessionInfo() {
        log.debug("[Memory]    Active sessions={}", activeSessions.size());
        return Map.of(
                "activeSessions", activeSessions.size(),
                "sessionIds", List.copyOf(activeSessions)
        );
    }
}
