package com.example.rag_spring_ai.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for endpoints that accept a generic search/lookup query.
 */
public record QueryRequest(@NotBlank String query) {}

