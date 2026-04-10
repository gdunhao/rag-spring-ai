package com.example.rag_spring_ai.ingestion;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Document Ingestion demo.
 *
 * Endpoints:
 *   POST /api/ingest/text           — Ingest a plain text document
 *   POST /api/ingest/json           — Ingest a JSON document
 *   POST /api/ingest/custom-chunking — Ingest with custom chunk sizes
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/text")
    public Map<String, Object> ingestText() {
        return ingestionService.ingestText();
    }

    @PostMapping("/json")
    public Map<String, Object> ingestJson() {
        return ingestionService.ingestJson();
    }

    @PostMapping("/custom-chunking")
    public Map<String, Object> ingestCustomChunking(
            @RequestParam(defaultValue = "400") int chunkSize,
            @RequestParam(defaultValue = "50") int minChunkSize) {
        return ingestionService.ingestWithCustomChunking(chunkSize, minChunkSize);
    }
}

