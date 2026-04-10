package com.example.rag_spring_ai.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for endpoints that accept a single natural-language question.
 */
public record QuestionRequest(@NotBlank String question) {}

