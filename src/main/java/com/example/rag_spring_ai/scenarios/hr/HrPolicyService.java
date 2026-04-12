package com.example.rag_spring_ai.scenarios.hr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rag_spring_ai.model.PolicyInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * REAL-WORLD SCENARIO: HR Policy Q&A System
 * ==========================================
 *
 * Conversational HR assistant with session memory.
 *
 * Memory pattern: shared {@link InMemoryChatMemoryRepository} backs all sessions;
 * the session ID is threaded through as an advisor param at call time via
 * {@link ChatMemory#CONVERSATION_ID} — no ConcurrentHashMap or per-request
 * ChatClient builds.
 */
@Service
public class HrPolicyService {

    private static final Logger log = LoggerFactory.getLogger(HrPolicyService.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource hrDocument;
    private final ChatMemory chatMemory;
    private QuestionAnswerAdvisor hrAdvisor;

    public HrPolicyService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/hr/hr-policies.txt") Resource hrDocument) {
        this.embeddingModel = embeddingModel;
        this.hrDocument = hrDocument;

        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
        log.info("[Memory]    HrPolicyService initialised | store=InMemoryChatMemoryRepository");
    }

    @PostConstruct
    public void init() {
        log.info("[INGESTION] Loading HR policies | source=hr-policies.txt");
        VectorStore hrStore = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(hrDocument);
        reader.getCustomMetadata().put("source", "hr-policies");
        reader.getCustomMetadata().put("type", "hr");
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(100)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(reader.get());
        log.info("[→VectorDB] Storing {} HR policy chunks (in-memory)", chunks.size());
        long t0 = System.currentTimeMillis();
        hrStore.add(chunks);
        log.info("[←VectorDB] HR policy store ready | chunks={} | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);
        this.hrAdvisor = QuestionAnswerAdvisor.builder(hrStore).build();
    }

    /**
     * Conversational HR Q&A with session memory.
     */
    public Map<String, String> askHr(String sessionId, String question) {
        log.info("[Memory]    HR session | sessionId={} | question='{}'", sessionId, question);
        log.info("[→VectorDB] Similarity search | collection=hr-policies | sessionId={}", sessionId);
        log.info("[→Ollama]   Chat request (HR+Memory) | model=qwen3:4b | sessionId={}", sessionId);
        long t0 = System.currentTimeMillis();

        String answer = chatClient.prompt()
                .system("""
                        You are an HR assistant for CloudFlow Inc. Help employees understand
                        company policies with warmth and clarity. Your guidelines:

                        1. Always cite the specific policy section in your answers
                        2. Use plain language — avoid HR jargon when possible
                        3. If a question involves a sensitive topic (termination, PIP, etc.),
                           be empathetic and suggest contacting HR directly
                        4. For complex scenarios, explain the general policy first, then note
                           that individual circumstances may vary
                        5. Remember the conversation context for follow-up questions
                        6. If unsure, say "I'd recommend checking with HR directly at
                           hr@cloudflow.io for your specific situation"
                        """)
                .advisors(hrAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(question)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms | sessionId={}",
                answer == null ? 0 : answer.length(), System.currentTimeMillis() - t0, sessionId);
        return Map.of("sessionId", sessionId, "question", question, "answer", answer);
    }

    /**
     * Get a structured summary of a specific policy topic.
     */
    public PolicyInfo getPolicyInfo(String topic) {
        log.info("[→VectorDB] Similarity search | structured=PolicyInfo | topic='{}'", topic);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=PolicyInfo | topic='{}'", topic);
        long t0 = System.currentTimeMillis();

        PolicyInfo info = chatClient.prompt()
                .system("""
                        You are an HR policy analyst. Extract structured information about the
                        requested policy topic from the employee handbook. Provide the policy
                        name, a clear summary, eligibility criteria, and key points.
                        """)
                .advisors(hrAdvisor)
                .user("Extract policy details about: " + topic)
                .call()
                .entity(PolicyInfo.class);

        log.info("[←Ollama]   Structured PolicyInfo received | elapsed={}ms", System.currentTimeMillis() - t0);
        return info;
    }

    /**
     * Quick answer — single-turn Q&A without memory.
     */
    public String quickAnswer(String question) {
        log.info("[→VectorDB] Similarity search | collection=hr-policies | question='{}'", question);
        log.info("[→Ollama]   Chat request (quick, no memory) | model=qwen3:4b | question='{}'", question);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .system("You are an HR assistant. Answer concisely based on the policy context.")
                .advisors(hrAdvisor)
                .user(question)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }
}
