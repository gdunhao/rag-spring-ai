package com.example.rag_spring_ai.multidoc;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Multi-Document RAG demo.
 *
 * Endpoints:
 *   GET  /api/multidoc/collections               — List available collections
 *   POST /api/multidoc/query/{collection}         — Query a specific collection
 *   POST /api/multidoc/smart-query                — Auto-route to best collection
 */
@Validated
@RestController
@RequestMapping("/api/multidoc")
public class MultiDocController {

    private final MultiDocService multiDocService;

    public MultiDocController(MultiDocService multiDocService) {
        this.multiDocService = multiDocService;
    }

    @GetMapping("/collections")
    public List<Map<String, String>> listCollections() {
        return multiDocService.listCollections();
    }

    @PostMapping("/query/{collection}")
    public Map<String, String> queryCollection(
            @PathVariable String collection,
            @Valid @RequestBody QuestionRequest request) {
        String answer = multiDocService.queryCollection(collection, request.question());
        return Map.of("collection", collection, "question", request.question(), "answer", answer);
    }

    @PostMapping("/smart-query")
    public Map<String, String> smartQuery(@Valid @RequestBody QuestionRequest request) {
        return multiDocService.smartQuery(request.question());
    }
}
