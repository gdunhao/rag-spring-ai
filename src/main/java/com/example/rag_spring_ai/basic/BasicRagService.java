package com.example.rag_spring_ai.basic;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service demonstrating basic RAG (Retrieval-Augmented Generation).
 *
 * This is the simplest form of RAG:
 * 1. Load a document → split into chunks → store embeddings
 * 2. When a question arrives, retrieve relevant chunks and feed them to the LLM
 *
 * Spring AI's {@link QuestionAnswerAdvisor} handles steps 2 automatically.
 */
@Service
public class BasicRagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Resource overviewDocument;
    // AtomicBoolean ensures the one-time ingestion guard is safe under concurrent requests.
    private final AtomicBoolean ingested = new AtomicBoolean(false);

    public BasicRagService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            @Value("classpath:documents/sample/spring-ai-overview.txt") Resource overviewDocument) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are an expert on Spring AI. Answer questions using the provided context.")
                .build();
        this.vectorStore = vectorStore;
        this.overviewDocument = overviewDocument;
    }

    /**
     * Ensure documents are ingested before querying.
     * Uses compareAndSet so only the first concurrent caller performs the work.
     */
    public void ingestDocuments() {
        if (!ingested.compareAndSet(false, true)) return;

        // 1. Read the document
        var reader = new TextReader(overviewDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        List<Document> documents = reader.get();

        // 2. Split into chunks (default: 800 tokens per chunk, 350 overlap)
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        // 3. Store in vector store (embeddings are computed automatically)
        vectorStore.add(chunks);
    }

    /**
     * Ask a question using basic RAG.
     * The QuestionAnswerAdvisor automatically retrieves relevant documents
     * and includes them in the prompt context.
     */
    public String ask(String question) {
        ingestDocuments();

        return chatClient.prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .user(question)
                .call()
                .content();
    }
}

