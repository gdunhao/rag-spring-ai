package com.example.rag_spring_ai.model;

/**
 * Structured output record for HR policy information.
 *
 * @param policyName  The name of the HR policy
 * @param summary     Brief summary of the policy
 * @param eligibility Who is eligible
 * @param keyPoints   Comma-separated key points
 */
public record PolicyInfo(
        String policyName,
        String summary,
        String eligibility,
        String keyPoints
) {}

