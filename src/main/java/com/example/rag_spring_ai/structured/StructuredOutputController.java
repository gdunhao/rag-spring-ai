package com.example.rag_spring_ai.structured;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.ApiEndpoint;
import com.example.rag_spring_ai.model.FaqEntry;
import com.example.rag_spring_ai.model.LegalClause;
import com.example.rag_spring_ai.model.QueryRequest;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Structured Output demo.
 *
 * Endpoints:
 *   POST /api/structured/faq    — Extract a structured FAQ entry
 *   POST /api/structured/legal  — Extract structured legal clauses
 *   POST /api/structured/api    — Extract structured API endpoint docs
 */
@Validated
@RestController
@RequestMapping("/api/structured")
public class StructuredOutputController {

    private final StructuredOutputService structuredOutputService;

    public StructuredOutputController(StructuredOutputService structuredOutputService) {
        this.structuredOutputService = structuredOutputService;
    }

    @PostMapping("/faq")
    public FaqEntry faq(@Valid @RequestBody QuestionRequest request) {
        return structuredOutputService.extractFaqEntry(request.question());
    }

    @PostMapping("/legal")
    public List<LegalClause> legal(@Valid @RequestBody QueryRequest request) {
        return structuredOutputService.extractLegalClauses(request.query());
    }

    @PostMapping("/api")
    public List<ApiEndpoint> api(@Valid @RequestBody QueryRequest request) {
        return structuredOutputService.extractApiEndpoints(request.query());
    }
}
