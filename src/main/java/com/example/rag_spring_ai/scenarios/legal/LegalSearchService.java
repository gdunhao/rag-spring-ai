package com.example.rag_spring_ai.scenarios.legal;

import com.example.rag_spring_ai.model.LegalClause;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * REAL-WORLD SCENARIO: Legal Document Search
 * ============================================
 *
 * This scenario demonstrates using RAG to search and analyze legal documents.
 * Common use cases include:
 *
 * - Contract clause search: "What does the contract say about liability?"
 * - Compliance checks: "Are we GDPR compliant according to our ToS?"
 * - Risk analysis: "What termination clauses exist?"
 * - Structured extraction: Get clause details in a structured format
 *
 * NOTE: {@link ChatClient} is built once; the {@link QuestionAnswerAdvisor} is
 * pre-built in {@code @PostConstruct} — no allocations per request.
 */
@Service
public class LegalSearchService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource legalDocument;
    private QuestionAnswerAdvisor legalAdvisor;

    public LegalSearchService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/legal/terms-of-service.txt") Resource legalDocument) {
        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;
        this.legalDocument = legalDocument;
    }

    @PostConstruct
    public void init() {
        VectorStore legalStore = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(legalDocument);
        reader.getCustomMetadata().put("source", "terms-of-service");
        reader.getCustomMetadata().put("documentType", "legal");
        // Smaller chunks for legal docs — more precise clause matching
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(100)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(reader.get());
        legalStore.add(chunks);
        this.legalAdvisor = QuestionAnswerAdvisor.builder(legalStore).build();
    }

    /**
     * Search for relevant legal clauses based on a natural language query.
     */
    public String searchClauses(String query) {
        return chatClient.prompt()
                .system("""
                        You are a legal document analyst. When asked about legal topics,
                        search the Terms of Service and provide accurate, specific answers.
                        Always cite the relevant section numbers. Use clear, non-legal
                        language when summarizing for non-lawyers. Note any important
                        caveats or conditions.
                        """)
                .advisors(legalAdvisor)
                .user(query)
                .call()
                .content();
    }

    /**
     * Extract structured legal clause information.
     */
    public List<LegalClause> extractStructuredClauses(String query) {
        return chatClient.prompt()
                .system("""
                        You are a legal document analyst. Extract relevant clauses from the
                        Terms of Service as structured data. For each clause, provide the
                        section number, title, a plain-English summary, and relevance level
                        (HIGH, MEDIUM, LOW) to the query.
                        """)
                .advisors(legalAdvisor)
                .user("Find all clauses related to: " + query)
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }

    /**
     * Perform a compliance check — analyze if a specific practice is compliant.
     */
    public Map<String, String> complianceCheck(String practice) {
        String analysis = chatClient.prompt()
                .system("""
                        You are a compliance analyst. Based on the Terms of Service, determine
                        whether the described practice is compliant or non-compliant. Provide:
                        1. A clear COMPLIANT or NON-COMPLIANT verdict
                        2. The relevant sections that support your conclusion
                        3. Any recommendations for ensuring compliance
                        """)
                .advisors(legalAdvisor)
                .user("Is this practice compliant with our Terms of Service? Practice: " + practice)
                .call()
                .content();

        return Map.of("practice", practice, "analysis", analysis);
    }
}
