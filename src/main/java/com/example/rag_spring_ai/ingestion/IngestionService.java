package com.example.rag_spring_ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service demonstrating different document ingestion strategies.
 *
 * Spring AI supports multiple document readers:
 * - {@link TextReader}  — Plain text files (.txt)
 * - {@link JsonReader}  — JSON files (.json) with configurable field mapping
 * - PagePdfDocumentReader — PDF files (one Document per page)
 * - TikaDocumentReader  — Multi-format (PDF, DOCX, HTML, etc.) via Apache Tika
 *
 * After reading, documents are split into chunks using {@link TokenTextSplitter}
 * and stored in the vector store.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final Resource textDocument;
    private final Resource jsonDocument;

    public IngestionService(
            VectorStore vectorStore,
            @Value("classpath:documents/sample/spring-ai-overview.txt") Resource textDocument,
            @Value("classpath:documents/sample/ai-concepts.json") Resource jsonDocument) {
        this.vectorStore = vectorStore;
        this.textDocument = textDocument;
        this.jsonDocument = jsonDocument;
    }

    /**
     * Ingest a plain text file.
     * TextReader reads the entire file as a single Document.
     * We then split it into smaller chunks for better retrieval.
     */
    public Map<String, Object> ingestText() {
        log.info("[INGESTION] Starting text ingestion | source=spring-ai-overview.txt");
        var reader = new TextReader(textDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        reader.getCustomMetadata().put("type", "text");

        List<Document> documents = reader.get();
        log.debug("[INGESTION] Read {} raw document(s)", documents.size());

        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);
        log.info("[→VectorDB] Storing {} chunks | source=spring-ai-overview.txt | format=text", chunks.size());

        long t0 = System.currentTimeMillis();
        vectorStore.add(chunks);
        log.info("[←VectorDB] Stored {} chunks | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);

        return Map.of(
                "source", "spring-ai-overview.txt",
                "format", "text",
                "documentsRead", documents.size(),
                "chunksCreated", chunks.size(),
                "status", "ingested"
        );
    }

    /**
     * Ingest a JSON file.
     * JsonReader can extract specific fields from JSON arrays.
     * Each JSON object becomes a separate Document.
     */
    public Map<String, Object> ingestJson() {
        log.info("[INGESTION] Starting JSON ingestion | source=ai-concepts.json");
        var reader = new JsonReader(jsonDocument, "title", "content", "category");
        List<Document> documents = reader.get();
        log.debug("[INGESTION] Read {} JSON document(s)", documents.size());

        log.info("[→VectorDB] Storing {} documents | source=ai-concepts.json | format=json", documents.size());
        long t0 = System.currentTimeMillis();
        vectorStore.add(documents);
        log.info("[←VectorDB] Stored {} documents | elapsed={}ms", documents.size(), System.currentTimeMillis() - t0);

        return Map.of(
                "source", "ai-concepts.json",
                "format", "json",
                "documentsRead", documents.size(),
                "chunksCreated", documents.size(),
                "status", "ingested"
        );
    }

    /**
     * Ingest with custom chunking parameters.
     * TokenTextSplitter allows control over:
     * - defaultChunkSize:   target number of tokens per chunk
     * - minChunkSizeChars:  minimum chunk size in characters
     * - minChunkLengthToEmbed: minimum chunk length to create an embedding for
     * - maxNumChunks:       maximum number of chunks to create
     * - keepSeparator:      whether to keep paragraph separators
     */
    public Map<String, Object> ingestWithCustomChunking(int chunkSize, int minChunkSize) {
        log.info("[INGESTION] Starting custom-chunking ingestion | source=spring-ai-overview.txt | chunkSize={} | minChunkSize={}", chunkSize, minChunkSize);
        var reader = new TextReader(textDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        reader.getCustomMetadata().put("chunking", "custom");

        List<Document> documents = reader.get();
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(minChunkSize)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(100)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(documents);
        log.info("[→VectorDB] Storing {} chunks | chunkSize={} | minChunkSize={}", chunks.size(), chunkSize, minChunkSize);

        long t0 = System.currentTimeMillis();
        vectorStore.add(chunks);
        log.info("[←VectorDB] Stored {} chunks | elapsed={}ms", chunks.size(), System.currentTimeMillis() - t0);

        return Map.of(
                "source", "spring-ai-overview.txt",
                "format", "text",
                "chunkSize", chunkSize,
                "minChunkSize", minChunkSize,
                "documentsRead", documents.size(),
                "chunksCreated", chunks.size(),
                "status", "ingested"
        );
    }
}
