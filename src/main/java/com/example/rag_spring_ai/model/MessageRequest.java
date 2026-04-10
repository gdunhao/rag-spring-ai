package com.example.rag_spring_ai.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for conversational/chat endpoints.
 */
public record MessageRequest(@NotBlank String message) {}

