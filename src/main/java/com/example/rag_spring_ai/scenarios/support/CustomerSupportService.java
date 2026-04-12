package com.example.rag_spring_ai.scenarios.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rag_spring_ai.function.FunctionConfig;
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
 * REAL-WORLD SCENARIO: Customer Support Bot
 * ==========================================
 *
 * Features: FAQ Knowledge Base, Conversational Memory, Function Calling (ticket creation).
 *
 * Memory pattern: a shared {@link InMemoryChatMemoryRepository} backs all sessions.
 * The session ID is threaded through via {@link ChatMemory#CONVERSATION_ID} as an
 * advisor param at call time — no ConcurrentHashMap or per-request ChatClient builds.
 */
@Service
public class CustomerSupportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportService.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource faqDocument;
    private final FunctionConfig.SupportTools supportTools;
    private final ChatMemory chatMemory;
    private QuestionAnswerAdvisor faqAdvisor;

    public CustomerSupportService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            FunctionConfig.SupportTools supportTools,
            @Value("classpath:documents/faq/customer-faq.txt") Resource faqDocument) {
        this.embeddingModel = embeddingModel;
        this.supportTools = supportTools;
        this.faqDocument = faqDocument;

        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a friendly and professional customer support agent for CloudFlow,
                        a cloud-based collaboration platform. Your responsibilities:

                        1. Answer questions using the FAQ knowledge base provided as context
                        2. Be empathetic and understanding with frustrated customers
                        3. Provide specific, actionable answers (include links, steps, etc.)
                        4. If you cannot answer from the FAQ, apologize and offer to create a
                           support ticket using the createTicket function
                        5. Always greet returning customers warmly
                        6. If a customer asks about pricing, clearly explain all plan options

                        Remember the conversation history to provide continuity.
                        Always sign off as "CloudFlow Support Team".
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
        log.info("[Memory]    CustomerSupportService initialised | store=InMemoryChatMemoryRepository | tools=SupportTools");
    }

    @PostConstruct
    public void init() {
        log.info("[INGESTION] Loading customer FAQ | source=customer-faq.txt");
        VectorStore faqStore = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(faqDocument);
        reader.getCustomMetadata().put("source", "customer-faq");
        reader.getCustomMetadata().put("type", "faq");
        List<Document> chunks = new TokenTextSplitter().apply(reader.get());
        log.info("[→VectorDB] Storing {} FAQ chunks (in-memory)", chunks.size());
        long t0 = System.currentTimeMillis();
        faqStore.add(chunks);
        log.info("[←VectorDB] FAQ store ready | chunks={} | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);
        this.faqAdvisor = QuestionAnswerAdvisor.builder(faqStore).build();
    }

    /**
     * Handle a customer support message within a session.
     * The conversation ID is passed as an advisor param — no new ChatClient per request.
     */
    public Map<String, String> handleMessage(String sessionId, String message) {
        log.info("[Memory]    Support session | sessionId={} | message='{}'", sessionId, message);
        log.info("[→VectorDB] Similarity search | collection=customer-faq | sessionId={}", sessionId);
        log.info("[→Ollama]   Chat request (Support+Memory+Tools) | model=qwen3:4b | sessionId={}", sessionId);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .advisors(faqAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .tools(supportTools)
                .user(message)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms | sessionId={}",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0, sessionId);
        return Map.of("sessionId", sessionId, "message", message, "response", response);
    }

    /**
     * End a support session by clearing its memory.
     */
    public void endSession(String sessionId) {
        chatMemory.clear(sessionId);
        log.info("[Memory]    Session ENDED | sessionId={}", sessionId);
    }
}
