package com.example.rag_spring_ai.multidoc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiDocService keyword-routing logic.
 *
 * The smartQuery method routes questions to the correct collection based on
 * keyword heuristics. These tests validate that routing without loading Spring.
 */
class MultiDocServiceSmartQueryTest {

    private final KeywordRouter router = new KeywordRouter();

    /**
     * Thin helper that reproduces the exact same routing logic from MultiDocService
     * so we can test it independently from all the Spring / AI wiring.
     */
    static class KeywordRouter {
        String detect(String question) {
            String lower = question.toLowerCase();
            if (lower.contains("price") || lower.contains("plan") || lower.contains("billing")
                    || lower.contains("feature") || lower.contains("support")) {
                return "faq";
            } else if (lower.contains("terms") || lower.contains("legal") || lower.contains("liability")
                    || lower.contains("policy") || lower.contains("compliance") || lower.contains("refund")) {
                return "legal";
            } else if (lower.contains("api") || lower.contains("endpoint") || lower.contains("webhook")
                    || lower.contains("auth") || lower.contains("rest")) {
                return "tech";
            } else if (lower.contains("pto") || lower.contains("leave") || lower.contains("salary")
                    || lower.contains("benefits") || lower.contains("hr") || lower.contains("employee")) {
                return "hr";
            } else {
                return "faq"; // default
            }
        }
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
            "What are your pricing plans?,        faq",
            "How do I cancel my billing?,         faq",
            "What features does CloudFlow have?,  faq",
            "I need support with my account,      faq",
            "What are the terms of service?,      legal",
            "Is there a refund policy?,           legal",
            "Compliance requirements for GDPR,    legal",
            "What is our liability cap?,          legal",
            "How do I use the REST API?,          tech",
            "List all available endpoints,        tech",
            "How do webhooks work?,               tech",
            "How do I authenticate with OAuth?,   tech",
            "How many PTO days do I have?,        hr",
            "What are the leave policies?,        hr",
            "What is my salary range?,            hr",
            "Employee benefits overview,          hr",
    })
    void smartRouting_routesToCorrectCollection(String question, String expected) {
        assertThat(router.detect(question.trim())).isEqualTo(expected.trim());
    }

    @Test
    void unknownQuestion_defaultsToFaq() {
        assertThat(router.detect("What is the meaning of life?")).isEqualTo("faq");
    }

    @Test
    void routing_isCaseInsensitive() {
        assertThat(router.detect("WHAT ARE THE TERMS?")).isEqualTo("legal");
        assertThat(router.detect("REST API DOCUMENTATION")).isEqualTo("tech");
    }
}

