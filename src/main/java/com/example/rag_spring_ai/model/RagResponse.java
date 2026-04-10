package com.example.rag_spring_ai.model;

import java.util.List;

/**
 * Generic response wrapper for RAG query results.
 *
 * @param answer         The generated answer from the LLM
 * @param sourceDocuments List of source document identifiers used to generate the answer
 */
public record RagResponse(
        String answer,
        List<String> sourceDocuments
) {}

