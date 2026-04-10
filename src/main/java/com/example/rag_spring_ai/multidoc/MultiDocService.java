package com.example.rag_spring_ai.multidoc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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
 * Service demonstrating Multi-Document RAG with separate collections.
 *
 * In real applications, you often have different document collections that
 * need to be queried independently or together:
 * - FAQ documents for customer support
 * - Legal documents for compliance
 * - Technical docs for developers
 * - HR policies for employees
 *
 * Each collection gets its own in-memory {@link SimpleVectorStore} and a
 * pre-built {@link QuestionAnswerAdvisor}; the single {@link ChatClient} is
 * reused across all queries, with the system prompt set per-call.
 */
@Service
public class MultiDocService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    private final Resource faqDocument;
    private final Resource legalDocument;
    private final Resource techDocument;
    private final Resource hrDocument;

    // Pre-built advisors per collection — avoids re-creating on every request
    private QuestionAnswerAdvisor faqAdvisor;
    private QuestionAnswerAdvisor legalAdvisor;
    private QuestionAnswerAdvisor techAdvisor;
    private QuestionAnswerAdvisor hrAdvisor;

    public MultiDocService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/faq/customer-faq.txt") Resource faqDocument,
            @Value("classpath:documents/legal/terms-of-service.txt") Resource legalDocument,
            @Value("classpath:documents/techdocs/api-guide.txt") Resource techDocument,
            @Value("classpath:documents/hr/hr-policies.txt") Resource hrDocument) {
        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;
        this.faqDocument = faqDocument;
        this.legalDocument = legalDocument;
        this.techDocument = techDocument;
        this.hrDocument = hrDocument;
    }

    @PostConstruct
    public void init() {
        faqAdvisor    = advisorFor(createAndIngest(faqDocument,    "customer-faq",     "faq"));
        legalAdvisor  = advisorFor(createAndIngest(legalDocument,  "terms-of-service", "legal"));
        techAdvisor   = advisorFor(createAndIngest(techDocument,   "api-guide",        "technical"));
        hrAdvisor     = advisorFor(createAndIngest(hrDocument,     "hr-policies",      "hr"));
    }

    private VectorStore createAndIngest(Resource resource, String source, String collection) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(resource);
        reader.getCustomMetadata().put("source", source);
        reader.getCustomMetadata().put("collection", collection);
        List<Document> chunks = new TokenTextSplitter().apply(reader.get());
        store.add(chunks);
        return store;
    }

    private QuestionAnswerAdvisor advisorFor(VectorStore store) {
        return new QuestionAnswerAdvisor(store);
    }

    /**
     * Query a specific document collection.
     */
    public String queryCollection(String collection, String question) {
        QuestionAnswerAdvisor advisor = switch (collection.toLowerCase()) {
            case "faq"                  -> faqAdvisor;
            case "legal"                -> legalAdvisor;
            case "tech", "technical"    -> techAdvisor;
            case "hr"                   -> hrAdvisor;
            default -> throw new IllegalArgumentException("Unknown collection: " + collection);
        };

        return chatClient.prompt()
                .system("Answer using only the provided " + collection + " documentation context.")
                .advisors(advisor)
                .user(question)
                .call()
                .content();
    }

    /**
     * Smart routing: automatically detect which collection to query based on
     * the question content using keyword heuristics.
     */
    public Map<String, String> smartQuery(String question) {
        String lower = question.toLowerCase();
        String detectedCollection;

        if (lower.contains("price") || lower.contains("plan") || lower.contains("billing")
                || lower.contains("feature") || lower.contains("support")) {
            detectedCollection = "faq";
        } else if (lower.contains("terms") || lower.contains("legal") || lower.contains("liability")
                || lower.contains("policy") || lower.contains("compliance") || lower.contains("refund")) {
            detectedCollection = "legal";
        } else if (lower.contains("api") || lower.contains("endpoint") || lower.contains("webhook")
                || lower.contains("auth") || lower.contains("rest")) {
            detectedCollection = "tech";
        } else if (lower.contains("pto") || lower.contains("leave") || lower.contains("salary")
                || lower.contains("benefits") || lower.contains("hr") || lower.contains("employee")) {
            detectedCollection = "hr";
        } else {
            detectedCollection = "faq"; // default
        }

        String answer = queryCollection(detectedCollection, question);
        return Map.of(
                "question", question,
                "detectedCollection", detectedCollection,
                "answer", answer
        );
    }

    /**
     * List available collections.
     */
    public List<Map<String, String>> listCollections() {
        return List.of(
                Map.of("name", "faq",   "description", "Customer FAQ — pricing, features, support"),
                Map.of("name", "legal", "description", "Terms of Service — legal agreements and policies"),
                Map.of("name", "tech",  "description", "API Documentation — REST endpoints and authentication"),
                Map.of("name", "hr",    "description", "HR Policies — PTO, benefits, conduct, compensation")
        );
    }
}

