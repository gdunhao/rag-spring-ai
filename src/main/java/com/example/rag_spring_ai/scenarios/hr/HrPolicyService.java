package com.example.rag_spring_ai.scenarios.hr;

import com.example.rag_spring_ai.model.PolicyInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @PostConstruct
    public void init() {
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
        hrStore.add(chunks);
        this.hrAdvisor = QuestionAnswerAdvisor.builder(hrStore).build();
    }

    /**
     * Conversational HR Q&A with session memory.
     */
    public Map<String, String> askHr(String sessionId, String question) {
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

        return Map.of("sessionId", sessionId, "question", question, "answer", answer);
    }

    /**
     * Get a structured summary of a specific policy topic.
     */
    public PolicyInfo getPolicyInfo(String topic) {
        return chatClient.prompt()
                .system("""
                        You are an HR policy analyst. Extract structured information about the
                        requested policy topic from the employee handbook. Provide the policy
                        name, a clear summary, eligibility criteria, and key points.
                        """)
                .advisors(hrAdvisor)
                .user("Extract policy details about: " + topic)
                .call()
                .entity(PolicyInfo.class);
    }

    /**
     * Quick answer — single-turn Q&A without memory.
     */
    public String quickAnswer(String question) {
        return chatClient.prompt()
                .system("You are an HR assistant. Answer concisely based on the policy context.")
                .advisors(hrAdvisor)
                .user(question)
                .call()
                .content();
    }
}
