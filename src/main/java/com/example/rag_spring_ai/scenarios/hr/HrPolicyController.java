package com.example.rag_spring_ai.scenarios.hr;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.PolicyInfo;
import com.example.rag_spring_ai.model.QuestionRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the HR Policy Q&A scenario.
 *
 * Endpoints:
 *   POST /api/scenarios/hr/chat/{sessionId} — Conversational HR Q&A
 *   POST /api/scenarios/hr/policy           — Get structured policy info
 *   POST /api/scenarios/hr/quick            — Quick single-turn Q&A
 */
@Validated
@RestController
@RequestMapping("/api/scenarios/hr")
public class HrPolicyController {

    private final HrPolicyService hrPolicyService;

    public HrPolicyController(HrPolicyService hrPolicyService) {
        this.hrPolicyService = hrPolicyService;
    }

    @PostMapping("/chat/{sessionId}")
    public Map<String, String> chat(
            @PathVariable String sessionId,
            @Valid @RequestBody QuestionRequest request) {
        return hrPolicyService.askHr(sessionId, request.question());
    }

    /** Request body for the policy-info endpoint. */
    public record TopicRequest(@NotBlank String topic) {}

    @PostMapping("/policy")
    public PolicyInfo getPolicy(@Valid @RequestBody TopicRequest request) {
        return hrPolicyService.getPolicyInfo(request.topic());
    }

    @PostMapping("/quick")
    public Map<String, String> quickAnswer(@Valid @RequestBody QuestionRequest request) {
        String answer = hrPolicyService.quickAnswer(request.question());
        return Map.of("question", request.question(), "answer", answer);
    }
}
