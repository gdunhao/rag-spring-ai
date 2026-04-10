package com.example.rag_spring_ai.metadata;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service demonstrating Metadata Filtering in vector store queries.
 *
 * Documents stored in a vector store can have metadata (key-value pairs) attached
 * to them. When performing similarity search, you can filter results based on
 * metadata values. This is extremely powerful for:
 *
 * - Multi-tenant systems (filter by tenant ID)
 * - Document versioning (filter by version or date)
 * - Category-based retrieval (filter by document type/category)
 * - Access control (filter by permission level)
 *
 * Spring AI provides {@link FilterExpressionBuilder} for building type-safe
 * filter expressions.
 *
 * NOTE: The {@code boolean ingested} flag is removed — Spring guarantees
 * {@code @PostConstruct} runs exactly once on a singleton bean.
 */
@Service
public class MetadataFilterService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public MetadataFilterService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // Build once in the constructor — reused across all requests
        this.chatClient = chatClientBuilder.build();
    }

    @PostConstruct
    public void ingestDocumentsWithMetadata() {
        List<Document> documents = new ArrayList<>();

        // Product documentation — different versions and categories
        documents.add(new Document(
                "CloudFlow v2.0 introduced real-time collaboration with up to 50 simultaneous editors. It also added a new dashboard with customizable widgets and dark mode support.",
                Map.of("product", "cloudflow", "version", "2.0", "category", "release-notes", "year", "2025")
        ));
        documents.add(new Document(
                "CloudFlow v2.5 added AI-powered document summarization, smart search with natural language queries, and automated workflow templates. Performance improved by 40%.",
                Map.of("product", "cloudflow", "version", "2.5", "category", "release-notes", "year", "2026")
        ));
        documents.add(new Document(
                "CloudFlow's API rate limits are: Starter 100 req/min, Professional 1000 req/min, Enterprise 10000 req/min. Rate limit headers are included in all API responses.",
                Map.of("product", "cloudflow", "version", "2.5", "category", "api-docs", "year", "2026")
        ));
        documents.add(new Document(
                "DataSync Pro v1.0 is a real-time data synchronization tool that supports PostgreSQL, MySQL, MongoDB, and Redis. It provides change data capture (CDC) with sub-second latency.",
                Map.of("product", "datasync", "version", "1.0", "category", "release-notes", "year", "2025")
        ));
        documents.add(new Document(
                "DataSync Pro v1.5 added support for Apache Kafka as a streaming target, schema evolution handling, and a visual pipeline builder. It also introduced conflict resolution strategies.",
                Map.of("product", "datasync", "version", "1.5", "category", "release-notes", "year", "2026")
        ));
        documents.add(new Document(
                "Security best practices for CloudFlow: enable 2FA for all users, rotate API keys every 90 days, use IP allowlisting for API access, and review audit logs weekly.",
                Map.of("product", "cloudflow", "version", "2.5", "category", "security", "year", "2026")
        ));

        vectorStore.add(documents);
    }

    /**
     * Search with metadata filter — filter by product name.
     */
    public List<Map<String, Object>> searchByProduct(String query, String product) {
        var filter = new FilterExpressionBuilder();

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .filterExpression(filter.eq("product", product).build())
                        .build()
        );

        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getText(),
                        "metadata", doc.getMetadata()
                ))
                .toList();
    }

    /**
     * Search with metadata filter — filter by category.
     */
    public List<Map<String, Object>> searchByCategory(String query, String category) {
        var filter = new FilterExpressionBuilder();

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .filterExpression(filter.eq("category", category).build())
                        .build()
        );

        return results.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getText(),
                        "metadata", doc.getMetadata()
                ))
                .toList();
    }

    /**
     * RAG query with metadata filter — only retrieve context from a specific product.
     * The system prompt interpolates the product name; the ChatClient is reused.
     */
    public String askAboutProduct(String question, String product) {
        var filter = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(3)
                .filterExpression(filter.eq("product", product).build())
                .build();

        return chatClient.prompt()
                .system("Answer questions using only the provided context for the " + product + " product.")
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build())
                .user(question)
                .call()
                .content();
    }
}
