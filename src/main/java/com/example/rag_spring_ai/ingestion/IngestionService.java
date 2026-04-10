package com.example.rag_spring_ai.ingestion;

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
        var reader = new TextReader(textDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        reader.getCustomMetadata().put("type", "text");

        List<Document> documents = reader.get();
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);
        vectorStore.add(chunks);

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
        var reader = new JsonReader(jsonDocument, "title", "content", "category");
        List<Document> documents = reader.get();

        // JSON documents are typically small enough that chunking is optional
        vectorStore.add(documents);

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
        var reader = new TextReader(textDocument);
        reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
        reader.getCustomMetadata().put("chunking", "custom");

        List<Document> documents = reader.get();
        var splitter = new TokenTextSplitter(chunkSize, minChunkSize, 5, 100, true);
        List<Document> chunks = splitter.apply(documents);
        vectorStore.add(chunks);

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

