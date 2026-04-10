package com.example.rag_spring_ai.scenarios.support;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.MessageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Customer Support Bot scenario.
 *
 * Endpoints:
 *   POST   /api/scenarios/support/{sessionId}  — Send a support message
 *   DELETE /api/scenarios/support/{sessionId}   — End a support session
 */
@Validated
@RestController
@RequestMapping("/api/scenarios/support")
public class CustomerSupportController {

    private final CustomerSupportService supportService;

    public CustomerSupportController(CustomerSupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/{sessionId}")
    public Map<String, String> chat(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageRequest request) {
        return supportService.handleMessage(sessionId, request.message());
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, String> endSession(@PathVariable String sessionId) {
        supportService.endSession(sessionId);
        return Map.of("status", "Session ended", "sessionId", sessionId);
    }
}
