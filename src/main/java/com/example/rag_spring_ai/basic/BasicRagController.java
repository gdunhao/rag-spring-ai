package com.example.rag_spring_ai.basic;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller for the Basic RAG demo.
 *
 * Endpoints:
 *   POST /api/basic/ask    — Ask a question about Spring AI (RAG-powered)
 *   POST /api/basic/ingest — Manually trigger document ingestion
 *   POST /api/basic/upload — Upload and ingest a custom document file
 */
@Validated
@RestController
@RequestMapping("/api/basic")
public class BasicRagController {

    private final BasicRagService ragService;

    public BasicRagController(BasicRagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@Valid @RequestBody QuestionRequest request) {
        String answer = ragService.ask(request.question());
        return Map.of("question", request.question(), "answer", answer);
    }

    @PostMapping("/ingest")
    public Map<String, String> ingest() {
        ragService.ingestDocuments();
        return Map.of("status", "Documents ingested successfully");
    }

    /**
     * Accept a multipart file upload, ingest it into the shared vector store,
     * and return a summary of the operation.
     *
     * Supported formats: TXT, Markdown, PDF, DOCX, HTML, and anything Apache Tika can parse.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return Map.of("error", "No file content received. Please select a non-empty file.");
        }

        int chunks = ragService.ingestUploadedFile(file);
        return Map.of(
                "status",   "File ingested successfully",
                "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                "size",     file.getSize(),
                "chunks",   chunks
        );
    }
}
