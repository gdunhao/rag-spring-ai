package com.example.rag_spring_ai.structured;

import com.example.rag_spring_ai.model.ApiEndpoint;
import com.example.rag_spring_ai.model.FaqEntry;
import com.example.rag_spring_ai.model.LegalClause;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StructuredOutputController.class)
@ActiveProfiles("test")
class StructuredOutputControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StructuredOutputService structuredOutputService;

    @Test
    void faq_returnsStructuredFaqEntry() throws Exception {
        when(structuredOutputService.extractFaqEntry("What is pricing?"))
                .thenReturn(new FaqEntry("What is pricing?", "We offer three plans.", "billing"));

        mockMvc.perform(post("/api/structured/faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What is pricing?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is pricing?"))
                .andExpect(jsonPath("$.answer").value("We offer three plans."))
                .andExpect(jsonPath("$.category").value("billing"));
    }

    @Test
    void legal_returnsListOfLegalClauses() throws Exception {
        when(structuredOutputService.extractLegalClauses("liability"))
                .thenReturn(List.of(
                        new LegalClause("3.1", "Liability Cap", "Limits to direct damages.", "HIGH")
                ));

        mockMvc.perform(post("/api/structured/legal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": "liability"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].section").value("3.1"))
                .andExpect(jsonPath("$[0].relevance").value("HIGH"));
    }

    @Test
    void api_returnsListOfApiEndpoints() throws Exception {
        when(structuredOutputService.extractApiEndpoints("documents"))
                .thenReturn(List.of(
                        new ApiEndpoint("POST", "/api/documents", "Upload document", "file")
                ));

        mockMvc.perform(post("/api/structured/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": "documents"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].method").value("POST"))
                .andExpect(jsonPath("$[0].path").value("/api/documents"));
    }

    @Test
    void faq_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/structured/faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legal_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/structured/legal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


