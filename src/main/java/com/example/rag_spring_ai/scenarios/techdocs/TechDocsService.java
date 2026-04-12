package com.example.rag_spring_ai.scenarios.techdocs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rag_spring_ai.model.ApiEndpoint;
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
 * REAL-WORLD SCENARIO: Technical Documentation Assistant
 * =======================================================
 *
 * This scenario demonstrates a developer-facing RAG assistant that helps
 * navigate API documentation. Common use cases:
 *
 * - "How do I authenticate with the API?"
 * - "What endpoint should I use to upload documents?"
 * - "What are the rate limits for my plan?"
 * - Structured extraction of endpoint specifications
 *
 * NOTE: {@link ChatClient} is built once in the constructor; the
 * {@link QuestionAnswerAdvisor} is pre-built in {@code @PostConstruct}.
 */
@Service
public class TechDocsService {

    private static final Logger log = LoggerFactory.getLogger(TechDocsService.class);

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource apiGuide;
    private QuestionAnswerAdvisor techAdvisor;

    public TechDocsService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/techdocs/api-guide.txt") Resource apiGuide) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.embeddingModel = embeddingModel;
        this.apiGuide = apiGuide;
    }

    @PostConstruct
    public void init() {
        log.info("[INGESTION] Loading technical documentation | source=api-guide.txt");
        VectorStore techStore = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(apiGuide);
        reader.getCustomMetadata().put("source", "api-guide");
        reader.getCustomMetadata().put("type", "technical-documentation");
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(100)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(reader.get());
        log.info("[→VectorDB] Storing {} tech-doc chunks (in-memory)", chunks.size());
        long t0 = System.currentTimeMillis();
        techStore.add(chunks);
        log.info("[←VectorDB] Tech-doc store ready | chunks={} | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);
        this.techAdvisor = QuestionAnswerAdvisor.builder(techStore).build();
    }

    /**
     * Ask a question about the API documentation.
     */
    public String askTechQuestion(String question) {
        log.info("[→VectorDB] Similarity search | collection=api-guide | question='{}'", question);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | scenario=TechDocs | question='{}'", question);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .system("""
                        You are a developer advocate and API documentation expert for CloudFlow.
                        Help developers understand the API by providing:
                        1. Clear, concise answers with code examples when appropriate
                        2. Relevant endpoint details (method, path, parameters)
                        3. Authentication requirements
                        4. Rate limits and error codes when relevant
                        5. Best practices and tips

                        Format responses in a developer-friendly way with code snippets.
                        """)
                .advisors(techAdvisor)
                .user(question)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }

    /**
     * Find endpoints related to a feature and return structured data.
     */
    public List<ApiEndpoint> findEndpoints(String feature) {
        log.info("[→VectorDB] Similarity search | structured=List<ApiEndpoint> | feature='{}'", feature);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=List<ApiEndpoint> | feature='{}'", feature);
        long t0 = System.currentTimeMillis();

        List<ApiEndpoint> endpoints = chatClient.prompt()
                .system("""
                        You are an API documentation expert. Extract all relevant API endpoints
                        for the requested feature. Return structured data with the HTTP method,
                        path, description, and parameters for each endpoint.
                        """)
                .advisors(techAdvisor)
                .user("Find all API endpoints related to: " + feature)
                .call()
                .entity(new ParameterizedTypeReference<>() {});

        log.info("[←Ollama]   Structured List<ApiEndpoint> received | count={} | elapsed={}ms",
                endpoints == null ? 0 : endpoints.size(), System.currentTimeMillis() - t0);
        return endpoints;
    }

    /**
     * Generate a curl example for a specific API operation.
     */
    public String generateCurlExample(String operation) {
        log.info("[→VectorDB] Similarity search | collection=api-guide | operation='{}'", operation);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | scenario=CurlGenerator | operation='{}'", operation);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .system("""
                        You are an API documentation expert. Generate a complete, working curl
                        command example for the requested API operation. Include:
                        1. The correct HTTP method and URL
                        2. Required headers (Authorization, Content-Type)
                        3. Request body with sample data (if applicable)
                        4. A brief explanation of each part of the command
                        """)
                .advisors(techAdvisor)
                .user("Generate a curl example for: " + operation)
                .call()
                .content();

        log.info("[←Ollama]   Curl example received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }
}
