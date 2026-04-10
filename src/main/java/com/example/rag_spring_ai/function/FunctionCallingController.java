package com.example.rag_spring_ai.function;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.MessageRequest;
import com.example.rag_spring_ai.model.QuestionRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Function Calling demo.
 *
 * Endpoints:
 *   POST /api/function/support — Handle a support request (RAG + ticket creation)
 *   POST /api/function/ask     — Ask with multiple tools available
 */
@Validated
@RestController
@RequestMapping("/api/function")
public class FunctionCallingController {

    private final FunctionCallingService functionCallingService;

    public FunctionCallingController(FunctionCallingService functionCallingService) {
        this.functionCallingService = functionCallingService;
    }

    @PostMapping("/support")
    public Map<String, String> support(@Valid @RequestBody MessageRequest request) {
        String response = functionCallingService.handleSupportRequest(request.message());
        return Map.of("message", request.message(), "response", response);
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@Valid @RequestBody QuestionRequest request) {
        String answer = functionCallingService.askWithTools(request.question());
        return Map.of("question", request.question(), "answer", answer);
    }
}
