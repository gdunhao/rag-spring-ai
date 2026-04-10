package com.example.rag_spring_ai.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Metadata Filtering demo.
 *
 * Endpoints:
 *   GET  /api/metadata/search/product   — Search filtered by product
 *   GET  /api/metadata/search/category  — Search filtered by category
 *   POST /api/metadata/ask              — RAG query filtered by product
 */
@Validated
@RestController
@RequestMapping("/api/metadata")
public class MetadataFilterController {

    private final MetadataFilterService metadataFilterService;

    public MetadataFilterController(MetadataFilterService metadataFilterService) {
        this.metadataFilterService = metadataFilterService;
    }

    @GetMapping("/search/product")
    public List<Map<String, Object>> searchByProduct(
            @RequestParam String query,
            @RequestParam(defaultValue = "cloudflow") String product) {
        return metadataFilterService.searchByProduct(query, product);
    }

    @GetMapping("/search/category")
    public List<Map<String, Object>> searchByCategory(
            @RequestParam String query,
            @RequestParam(defaultValue = "release-notes") String category) {
        return metadataFilterService.searchByCategory(query, category);
    }

    /** Typed request body for the product-scoped RAG endpoint. */
    public record ProductQuestionRequest(@NotBlank String question,
                                         @NotBlank String product) {}

    @PostMapping("/ask")
    public Map<String, String> askAboutProduct(@Valid @RequestBody ProductQuestionRequest request) {
        String answer = metadataFilterService.askAboutProduct(request.question(), request.product());
        return Map.of("question", request.question(), "product", request.product(), "answer", answer);
    }
}
