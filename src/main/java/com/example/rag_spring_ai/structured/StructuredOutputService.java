package com.example.rag_spring_ai.structured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rag_spring_ai.model.ApiEndpoint;
import com.example.rag_spring_ai.model.FaqEntry;
import com.example.rag_spring_ai.model.LegalClause;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service demonstrating Structured Output from RAG queries.
 *
 * Instead of receiving free-text answers, you can instruct Spring AI to parse
 * the LLM response into typed Java objects. This uses:
 *
 * - {@code .entity(Class)} on the ChatClient to specify the output type
 * - Spring AI's {@link org.springframework.ai.converter.BeanOutputConverter}
 *   under the hood, which adds format instructions to the prompt and parses
 *   the JSON response into the target type.
 *
 * NOTE: {@link ChatClient} is built once in the constructor and reused across
 * all requests; the system prompt is overridden per-call via {@code .system()}.
 */
@Service
public class StructuredOutputService {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public StructuredOutputService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * Extract a structured FAQ entry from a question.
     */
    public FaqEntry extractFaqEntry(String question) {
        log.info("[→VectorDB] Similarity search | structured=FaqEntry | question='{}'", question);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=FaqEntry | question='{}'", question);
        long t0 = System.currentTimeMillis();

        FaqEntry entry = chatClient.prompt()
                .system("""
                        You are an FAQ specialist. Based on the retrieved context, create a
                        structured FAQ entry with the question, a clear answer, and the category.
                        """)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user(question)
                .call()
                .entity(FaqEntry.class);

        log.info("[←Ollama]   Structured FaqEntry received | elapsed={}ms", System.currentTimeMillis() - t0);
        return entry;
    }

    /**
     * Extract structured legal clause information.
     */
    public List<LegalClause> extractLegalClauses(String query) {
        log.info("[→VectorDB] Similarity search | structured=List<LegalClause> | query='{}'", query);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=List<LegalClause> | query='{}'", query);
        long t0 = System.currentTimeMillis();

        List<LegalClause> clauses = chatClient.prompt()
                .system("""
                        You are a legal document analyst. Based on the retrieved context,
                        extract relevant legal clauses. Return a list of clauses with their
                        section, title, summary, and relevance level (HIGH, MEDIUM, LOW).
                        """)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user("Find clauses related to: " + query)
                .call()
                .entity(new ParameterizedTypeReference<List<LegalClause>>() {});

        log.info("[←Ollama]   Structured List<LegalClause> received | count={} | elapsed={}ms",
                clauses == null ? 0 : clauses.size(), System.currentTimeMillis() - t0);
        return clauses;
    }

    /**
     * Extract structured API endpoint documentation.
     */
    public List<ApiEndpoint> extractApiEndpoints(String query) {
        log.info("[→VectorDB] Similarity search | structured=List<ApiEndpoint> | query='{}'", query);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | outputType=List<ApiEndpoint> | query='{}'", query);
        long t0 = System.currentTimeMillis();

        List<ApiEndpoint> endpoints = chatClient.prompt()
                .system("""
                        You are an API documentation expert. Based on the retrieved context,
                        extract the relevant API endpoints. For each endpoint, provide the
                        HTTP method, path, description, and parameters.
                        """)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user("Find API endpoints related to: " + query)
                .call()
                .entity(new ParameterizedTypeReference<List<ApiEndpoint>>() {});

        log.info("[←Ollama]   Structured List<ApiEndpoint> received | count={} | elapsed={}ms",
                endpoints == null ? 0 : endpoints.size(), System.currentTimeMillis() - t0);
        return endpoints;
    }
}
