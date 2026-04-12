package com.example.rag_spring_ai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service demonstrating Spring AI Advisors.
 *
 * Advisors are a middleware-like pattern in Spring AI that intercept and modify
 * the request/response pipeline. They follow the chain-of-responsibility pattern
 * and can be composed together.
 *
 * Built-in Advisors:
 * - {@link QuestionAnswerAdvisor}: Retrieves documents from a VectorStore and adds
 *   them as context to the prompt. This is the core RAG advisor.
 * - {@link SafeGuardAdvisor}: Blocks prompts containing sensitive or banned words.
 * - {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}:
 *   Maintains conversation history (see memory demo).
 *
 * You can also create custom advisors by implementing the RequestResponseAdvisor interface.
 *
 * NOTE: {@link ChatClient} is thread-safe and reusable; we build it once in the constructor
 * and override the system prompt per call via {@code .system()} on the prompt chain.
 */
@Service
public class AdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AdvisorService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // Build once — thread-safe, no per-request allocation
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * Demo 1: QuestionAnswerAdvisor with custom search parameters.
     * Controls how many documents are retrieved and the similarity threshold.
     * Note: .query() is intentionally omitted from SearchRequest — QuestionAnswerAdvisor
     * overrides it with the live user message at runtime.
     */
    public String askWithCustomRetrieval(String question, int topK, double threshold) {
        log.info("[→VectorDB] Similarity search | topK={} | threshold={} | question='{}'", topK, threshold, question);
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        log.info("[→Ollama]   Chat request | model=qwen3:4b | advisor=QuestionAnswerAdvisor");
        long t0 = System.currentTimeMillis();
        String response = chatClient.prompt()
                .system("Answer based on provided context. Cite sources when possible.")
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build())
                .user(question)
                .call()
                .content();
        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }

    // In Spring AI 1.0.x SafeGuardAdvisor returns this prefix instead of throwing
    private static final String SAFEGUARD_FAILURE_PREFIX =
            "I'm unable to respond to that due to sensitive content";

    /**
     * Demo 2: SafeGuardAdvisor — blocks prompts containing banned words.
     * Useful for content moderation before sending to the LLM.
     */
    public Map<String, String> askWithSafeGuard(String question) {
        log.info("[→Ollama]   Chat request | advisors=SafeGuardAdvisor+QuestionAnswerAdvisor | question='{}'", question);
        List<String> bannedWords = List.of("hack", "exploit", "injection", "bypass security");

        long t0 = System.currentTimeMillis();
        String answer = chatClient.prompt()
                .system("You are a helpful assistant.")
                .advisors(
                        new SafeGuardAdvisor(bannedWords),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .user(question)
                .call()
                .content();

        if (answer == null || answer.startsWith(SAFEGUARD_FAILURE_PREFIX)) {
            log.warn("[SafeGuard] Request BLOCKED | question='{}' | elapsed={}ms", question, System.currentTimeMillis() - t0);
            return Map.of(
                    "question", question,
                    "answer", "Request blocked by SafeGuard",
                    "blocked", "true",
                    "reason", "Sensitive content detected in request"
            );
        }
        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms | blocked=false",
                answer.length(), System.currentTimeMillis() - t0);
        return Map.of("question", question, "answer", answer, "blocked", "false");
    }

    /**
     * Demo 3: Multiple advisors composed together.
     * Advisors execute in order: SafeGuard first (blocks bad input),
     * then QuestionAnswerAdvisor (retrieves context), then the LLM call.
     */
    public String askWithComposedAdvisors(String question) {
        log.info("[→Ollama]   Chat request | advisors=SafeGuardAdvisor+QuestionAnswerAdvisor (composed) | question='{}'", question);
        List<String> bannedWords = List.of("hack", "exploit");

        long t0 = System.currentTimeMillis();
        String response = chatClient.prompt()
                .system("""
                        You are a secure, knowledgeable assistant. Answer questions using
                        the provided context. If the question seems potentially harmful,
                        politely decline and suggest appropriate resources instead.
                        """)
                .advisors(
                        new SafeGuardAdvisor(bannedWords),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .user(question)
                .call()
                .content();
        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }
}
