package com.example.rag_spring_ai.scenarios.techdocs;

import com.example.rag_spring_ai.model.ApiEndpoint;
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

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final Resource apiGuide;
    private QuestionAnswerAdvisor techAdvisor;

    public TechDocsService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            @Value("classpath:documents/techdocs/api-guide.txt") Resource apiGuide) {
        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;
        this.apiGuide = apiGuide;
    }

    @PostConstruct
    public void init() {
        VectorStore techStore = SimpleVectorStore.builder(embeddingModel).build();
        var reader = new TextReader(apiGuide);
        reader.getCustomMetadata().put("source", "api-guide");
        reader.getCustomMetadata().put("type", "technical-documentation");
        var splitter = new TokenTextSplitter(500, 50, 5, 100, true);
        List<Document> chunks = splitter.apply(reader.get());
        techStore.add(chunks);
        this.techAdvisor = new QuestionAnswerAdvisor(techStore);
    }

    /**
     * Ask a question about the API documentation.
     */
    public String askTechQuestion(String question) {
        return chatClient.prompt()
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
    }

    /**
     * Find endpoints related to a feature and return structured data.
     */
    public List<ApiEndpoint> findEndpoints(String feature) {
        return chatClient.prompt()
                .system("""
                        You are an API documentation expert. Extract all relevant API endpoints
                        for the requested feature. Return structured data with the HTTP method,
                        path, description, and parameters for each endpoint.
                        """)
                .advisors(techAdvisor)
                .user("Find all API endpoints related to: " + feature)
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }

    /**
     * Generate a curl example for a specific API operation.
     */
    public String generateCurlExample(String operation) {
        return chatClient.prompt()
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
    }
}
