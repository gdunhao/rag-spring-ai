package com.example.rag_spring_ai.scenarios.techdocs;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.ApiEndpoint;
import com.example.rag_spring_ai.model.QuestionRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Technical Documentation Assistant scenario.
 *
 * Endpoints:
 *   POST /api/scenarios/techdocs/ask        — Ask about the API
 *   POST /api/scenarios/techdocs/endpoints   — Find endpoints for a feature
 *   POST /api/scenarios/techdocs/curl        — Generate curl examples
 */
@Validated
@RestController
@RequestMapping("/api/scenarios/techdocs")
public class TechDocsController {

    private final TechDocsService techDocsService;

    public TechDocsController(TechDocsService techDocsService) {
        this.techDocsService = techDocsService;
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@Valid @RequestBody QuestionRequest request) {
        String answer = techDocsService.askTechQuestion(request.question());
        return Map.of("question", request.question(), "answer", answer);
    }

    /** Request body for the endpoint-discovery query. */
    public record FeatureRequest(@NotBlank String feature) {}

    @PostMapping("/endpoints")
    public List<ApiEndpoint> findEndpoints(@Valid @RequestBody FeatureRequest request) {
        return techDocsService.findEndpoints(request.feature());
    }

    /** Request body for the curl-example generator. */
    public record OperationRequest(@NotBlank String operation) {}

    @PostMapping("/curl")
    public Map<String, String> generateCurl(@Valid @RequestBody OperationRequest request) {
        String curlExample = techDocsService.generateCurlExample(request.operation());
        return Map.of("operation", request.operation(), "curl", curlExample);
    }
}
