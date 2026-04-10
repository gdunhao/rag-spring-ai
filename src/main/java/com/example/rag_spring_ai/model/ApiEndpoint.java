package com.example.rag_spring_ai.model;

/**
 * Structured output record for API endpoint documentation.
 *
 * @param method      HTTP method (GET, POST, PUT, DELETE)
 * @param path        The endpoint path
 * @param description What the endpoint does
 * @param parameters  Comma-separated list of parameters
 */
public record ApiEndpoint(
        String method,
        String path,
        String description,
        String parameters
) {}

