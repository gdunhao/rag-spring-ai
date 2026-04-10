package com.example.rag_spring_ai.basic;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Basic RAG demo.
 *
 * Endpoints:
 *   POST /api/basic/ask    — Ask a question about Spring AI (RAG-powered)
 *   POST /api/basic/ingest — Manually trigger document ingestion
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
}
