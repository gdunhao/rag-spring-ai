package com.example.rag_spring_ai.model;

/**
 * Structured output record for FAQ entries.
 *
 * @param question The FAQ question
 * @param answer   The FAQ answer
 * @param category The category this FAQ belongs to
 */
public record FaqEntry(
        String question,
        String answer,
        String category
) {}

