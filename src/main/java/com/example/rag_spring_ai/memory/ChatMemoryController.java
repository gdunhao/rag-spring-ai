package com.example.rag_spring_ai.memory;

import jakarta.validation.Valid;
import com.example.rag_spring_ai.model.MessageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Chat Memory demo.
 *
 * Endpoints:
 *   POST   /api/chat/{sessionId}        — Send a message with RAG + memory
 *   POST   /api/chat/{sessionId}/simple  — Send a message with memory only (no RAG)
 *   DELETE /api/chat/{sessionId}         — Clear session memory
 *   GET    /api/chat/sessions            — List active sessions
 */
@Validated
@RestController
@RequestMapping("/api/chat")
public class ChatMemoryController {

    private final ChatMemoryService chatMemoryService;

    public ChatMemoryController(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    @PostMapping("/{sessionId}")
    public Map<String, String> chat(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageRequest request) {
        String response = chatMemoryService.chat(sessionId, request.message());
        return Map.of("sessionId", sessionId, "message", request.message(), "response", response);
    }

    @PostMapping("/{sessionId}/simple")
    public Map<String, String> chatSimple(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageRequest request) {
        String response = chatMemoryService.chatWithoutRag(sessionId, request.message());
        return Map.of("sessionId", sessionId, "message", request.message(), "response", response);
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, String> clearSession(@PathVariable String sessionId) {
        chatMemoryService.clearSession(sessionId);
        return Map.of("status", "Session cleared", "sessionId", sessionId);
    }

    @GetMapping("/sessions")
    public Map<String, Object> sessions() {
        return chatMemoryService.getSessionInfo();
    }
}
