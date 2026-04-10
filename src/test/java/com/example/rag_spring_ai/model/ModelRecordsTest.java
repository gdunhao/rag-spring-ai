package com.example.rag_spring_ai.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for all model record types.
 * Records are data carriers; tests verify correct field mapping and accessor behaviour.
 */
class ModelRecordsTest {

    @Test
    void faqEntry_storesAllFields() {
        FaqEntry entry = new FaqEntry("What is CloudFlow?", "A cloud collaboration platform.", "general");

        assertThat(entry.question()).isEqualTo("What is CloudFlow?");
        assertThat(entry.answer()).isEqualTo("A cloud collaboration platform.");
        assertThat(entry.category()).isEqualTo("general");
    }

    @Test
    void faqEntry_nullFieldsPermitted() {
        FaqEntry entry = new FaqEntry(null, null, null);

        assertThat(entry.question()).isNull();
        assertThat(entry.answer()).isNull();
        assertThat(entry.category()).isNull();
    }

    @Test
    void legalClause_storesAllFields() {
        LegalClause clause = new LegalClause("Section 3.1", "Liability", "Limits liability to direct damages.", "HIGH");

        assertThat(clause.section()).isEqualTo("Section 3.1");
        assertThat(clause.title()).isEqualTo("Liability");
        assertThat(clause.summary()).isEqualTo("Limits liability to direct damages.");
        assertThat(clause.relevance()).isEqualTo("HIGH");
    }

    @Test
    void apiEndpoint_storesAllFields() {
        ApiEndpoint ep = new ApiEndpoint("POST", "/api/documents", "Upload a document", "file, metadata");

        assertThat(ep.method()).isEqualTo("POST");
        assertThat(ep.path()).isEqualTo("/api/documents");
        assertThat(ep.description()).isEqualTo("Upload a document");
        assertThat(ep.parameters()).isEqualTo("file, metadata");
    }

    @Test
    void policyInfo_storesAllFields() {
        PolicyInfo info = new PolicyInfo("PTO Policy", "Annual leave summary.", "Full-time employees", "15 days/year, accrued monthly");

        assertThat(info.policyName()).isEqualTo("PTO Policy");
        assertThat(info.summary()).isEqualTo("Annual leave summary.");
        assertThat(info.eligibility()).isEqualTo("Full-time employees");
        assertThat(info.keyPoints()).isEqualTo("15 days/year, accrued monthly");
    }

    @Test
    void questionRequest_storesQuestion() {
        QuestionRequest req = new QuestionRequest("What is RAG?");
        assertThat(req.question()).isEqualTo("What is RAG?");
    }

    @Test
    void messageRequest_storesMessage() {
        MessageRequest req = new MessageRequest("Hello!");
        assertThat(req.message()).isEqualTo("Hello!");
    }

    @Test
    void queryRequest_storesQuery() {
        QueryRequest req = new QueryRequest("search term");
        assertThat(req.query()).isEqualTo("search term");
    }

    @Test
    void records_haveCorrectEquality() {
        FaqEntry e1 = new FaqEntry("Q", "A", "cat");
        FaqEntry e2 = new FaqEntry("Q", "A", "cat");
        FaqEntry e3 = new FaqEntry("Q", "X", "cat");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1).isNotEqualTo(e3);
    }
}

