package com.example.rag_spring_ai.scenarios.legal;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.LegalClause;
import com.example.rag_spring_ai.model.QueryRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Legal Document Search scenario.
 *
 * Endpoints:
 *   POST /api/scenarios/legal/search     — Search legal clauses
 *   POST /api/scenarios/legal/extract    — Extract structured clause data
 *   POST /api/scenarios/legal/compliance — Check compliance of a practice
 */
@Validated
@RestController
@RequestMapping("/api/scenarios/legal")
public class LegalSearchController {

    private final LegalSearchService legalSearchService;

    public LegalSearchController(LegalSearchService legalSearchService) {
        this.legalSearchService = legalSearchService;
    }

    @PostMapping("/search")
    public Map<String, String> search(@Valid @RequestBody QueryRequest request) {
        String result = legalSearchService.searchClauses(request.query());
        return Map.of("query", request.query(), "result", result);
    }

    @PostMapping("/extract")
    public List<LegalClause> extract(@Valid @RequestBody QueryRequest request) {
        return legalSearchService.extractStructuredClauses(request.query());
    }

    /** Request body for the compliance check endpoint. */
    public record ComplianceRequest(@NotBlank String practice) {}

    @PostMapping("/compliance")
    public Map<String, String> compliance(@Valid @RequestBody ComplianceRequest request) {
        return legalSearchService.complianceCheck(request.practice());
    }
}
