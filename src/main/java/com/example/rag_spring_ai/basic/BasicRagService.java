package com.example.rag_spring_ai.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
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

    private static final Logger log = LoggerFactory.getLogger(BasicRagService.class);

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
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.vectorStore = vectorStore;
        this.overviewDocument = overviewDocument;
    }

    /**
     * Ensure documents are ingested before querying.
     * Uses compareAndSet so only the first concurrent caller performs the work.
     */
    public void ingestDocuments() {
        if (!ingested.compareAndSet(false, true)) {
            log.debug("[→VectorDB] Ingestion skipped — documents already stored");
            return;
        }

        log.info("[INGESTION] Loading document: spring-ai-overview.txt");
        var reader = new TextReader(overviewDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        List<Document> documents = reader.get();
        log.debug("[INGESTION] Read {} raw document(s)", documents.size());

        // 2. Split into chunks (default: 800 tokens per chunk, 350 overlap)
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);
        log.info("[→VectorDB] Storing {} chunks | source=spring-ai-overview.txt", chunks.size());

        long t0 = System.currentTimeMillis();
        // 3. Store in vector store (embeddings are computed automatically)
        vectorStore.add(chunks);
        log.info("[←VectorDB] Stored {} chunks | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);
    }

    /** Plain-text extensions that bypass Tika (faster, no parsing overhead). */
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "markdown", "text");

    /**
     * Ingest an uploaded file into the vector store.
     * <ul>
     *   <li>Plain-text / Markdown → {@link TextReader} (lightweight)</li>
     *   <li>PDF, DOCX, HTML, etc. → {@link TikaDocumentReader} (Apache Tika, multi-format)</li>
     * </ul>
     *
     * @param file the uploaded multipart file
     * @return the number of chunks stored
     */
    public int ingestUploadedFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";

        log.info("[INGESTION] Processing uploaded file: {} ({} bytes, ext={})",
                originalName, file.getSize(), ext);

        // Wrap byte array as a named Resource so readers can inspect the filename
        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return originalName; }
        };

        List<Document> rawDocs;
        if (TEXT_EXTENSIONS.contains(ext)) {
            var reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", originalName);
            rawDocs = reader.get();
        } else {
            // TikaDocumentReader handles PDF, DOCX, PPTX, HTML, ODT, …
            var reader = new TikaDocumentReader(resource);
            rawDocs = reader.get();
        }
        log.debug("[INGESTION] Read {} raw document(s) from '{}'", rawDocs.size(), originalName);

        // Tag every chunk with the original filename for traceability
        rawDocs.forEach(d -> d.getMetadata().put("source", originalName));

        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(rawDocs);
        log.info("[→VectorDB] Storing {} chunks from '{}' | ext={}", chunks.size(), originalName, ext);

        long t0 = System.currentTimeMillis();
        vectorStore.add(chunks);
        log.info("[←VectorDB] Stored {} chunks | source='{}' | elapsed={}ms",
                chunks.size(), originalName, System.currentTimeMillis() - t0);

        return chunks.size();
    }

    /**
     * Ask a question using basic RAG.
     * The QuestionAnswerAdvisor automatically retrieves relevant documents
     * and includes them in the prompt context.
     */
    public String ask(String question) {
        ingestDocuments();

        log.info("[→VectorDB] Similarity search via QuestionAnswerAdvisor | question='{}'", question);
        log.info("[→Ollama]   Chat request | model=qwen3:4b | question='{}'", question);
        long t0 = System.currentTimeMillis();

        String response = chatClient.prompt()
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user(question)
                .call()
                .content();

        log.info("[←Ollama]   Response received | chars={} | elapsed={}ms",
                response == null ? 0 : response.length(), System.currentTimeMillis() - t0);
        return response;
    }
}
