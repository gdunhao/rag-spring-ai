package com.example.rag_spring_ai.scenarios.legal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rag_spring_ai.model.LegalClause;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
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

    private static final Logger log = LoggerFactory.getLogger(LegalSearchService.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource legalDocument;
    private QuestionAnswerAdvisor legalAdvisor;

    public LegalSearchService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/legal/terms-of-service.txt") Resource legalDocument) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.embeddingModel = embeddingModel;
        this.legalDocument = legalDocument;
    }

    @PostConstruct
    public void init() {
        log.info("[INGESTION] Loading legal document | source=terms-of-service.txt");
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
        log.info("[→VectorDB] Storing {} legal chunks (in-memory, smaller chunks for clause precision)", chunks.size());
        long t0 = System.currentTimeMillis();
        legalStore.add(chunks);
        log.info("[←VectorDB] Legal store ready | chunks={} | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);
        this.legalAdvisor = QuestionAnswerAdvisor.builder(legalStore).build();
    }

    /**
     * Search for relevant legal clauses based on a natural language query.
     */
    public String searchClauses(String query) {
        log.info("[→VectorDB] Similarity search | collection=legal | query='{}'", query);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | scenario=LegalSearch | query='{}'", query);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
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

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }

    /**
     * Extract structured legal clause information.
     */
    public List<LegalClause> extractStructuredClauses(String query) {
        log.info("[→VectorDB] Similarity search | structured=List<LegalClause> | query='{}'", query);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=List<LegalClause> | query='{}'", query);
        long t0 = System.currentTimeMillis();

        List<LegalClause> clauses = chatClient.prompt()
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

        log.info("[←Ollama]   Structured List<LegalClause> received | count={} | elapsed={}ms",
                clauses == null ? 0 : clauses.size(), System.currentTimeMillis() - t0);
        return clauses;
    }

    /**
     * Perform a compliance check — analyze if a specific practice is compliant.
     */
    public Map<String, String> complianceCheck(String practice) {
        log.info("[→VectorDB] Similarity search | collection=legal | compliance check | practice='{}'", practice);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | scenario=ComplianceCheck | practice='{}'", practice);
        long t0 = System.currentTimeMillis();

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

        log.info("[←Ollama]   Compliance analysis received | chars={} | elapsed={}ms",
                analysis == null ? 0 : analysis.length(), System.currentTimeMillis() - t0);
        return Map.of("practice", practice, "analysis", analysis);
    }
}
