package com.example.rag_spring_ai.model;

/**
 * Structured output record for legal document clauses.
 *
 * @param section     The section name or number
 * @param title       The clause title
 * @param summary     A brief summary of the clause
 * @param relevance   How relevant this clause is to the query (HIGH, MEDIUM, LOW)
 */
public record LegalClause(
        String section,
        String title,
        String summary,
        String relevance
) {}

