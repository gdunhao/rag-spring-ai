package com.example.rag_spring_ai.vectorstore;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Vector Store demo.
 *
 * Endpoints:
 *   POST /api/vectorstore/add-samples       — Add sample documents
 *   GET  /api/vectorstore/search             — Similarity search
 *   GET  /api/vectorstore/search-threshold   — Search with similarity threshold
 *   GET  /api/vectorstore/embedding-info     — Inspect embedding dimensions
 */
@RestController
@RequestMapping("/api/vectorstore")
public class VectorStoreController {

    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/add-samples")
    public Map<String, Object> addSamples() {
        return vectorStoreService.addSampleDocuments();
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return vectorStoreService.search(query, topK);
    }

    @GetMapping("/search-threshold")
    public List<Map<String, Object>> searchWithThreshold(
            @RequestParam String query,
            @RequestParam(defaultValue = "0.7") double threshold) {
        return vectorStoreService.searchWithThreshold(query, threshold);
    }

    @GetMapping("/embedding-info")
    public Map<String, Object> embeddingInfo(@RequestParam(defaultValue = "Hello world") String text) {
        return vectorStoreService.getEmbeddingInfo(text);
    }
}

