package com.example.rag_spring_ai.advisor;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Advisors demo.
 *
 * Endpoints:
 *   POST /api/advisor/custom-retrieval — RAG with custom search parameters
 *   POST /api/advisor/safeguard        — RAG with content moderation
 *   POST /api/advisor/composed         — Multiple advisors composed together
 */
@Validated
@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

    private final AdvisorService advisorService;

    public AdvisorController(AdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @PostMapping("/custom-retrieval")
    public Map<String, String> customRetrieval(
            @Valid @RequestBody QuestionRequest request,
            @RequestParam(defaultValue = "3") int topK,
            @RequestParam(defaultValue = "0.5") double threshold) {
        String answer = advisorService.askWithCustomRetrieval(request.question(), topK, threshold);
        return Map.of("question", request.question(), "answer", answer);
    }

    @PostMapping("/safeguard")
    public Map<String, String> safeguard(@Valid @RequestBody QuestionRequest request) {
        return advisorService.askWithSafeGuard(request.question());
    }

    @PostMapping("/composed")
    public Map<String, String> composed(@Valid @RequestBody QuestionRequest request) {
        String answer = advisorService.askWithComposedAdvisors(request.question());
        return Map.of("question", request.question(), "answer", answer);
    }
}
