package com.example.rag_spring_ai.scenarios.legal;

import com.example.rag_spring_ai.model.LegalClause;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegalSearchController.class)
@ActiveProfiles("test")
class LegalSearchControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LegalSearchService legalSearchService;

    @Test
    void search_returnsQueryAndResult() throws Exception {
        when(legalSearchService.searchClauses("termination")).thenReturn("Section 8 covers termination.");

        mockMvc.perform(post("/api/scenarios/legal/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": "termination"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("termination"))
                .andExpect(jsonPath("$.result").value("Section 8 covers termination."));
    }

    @Test
    void extract_returnsListOfClauses() throws Exception {
        when(legalSearchService.extractStructuredClauses("liability"))
                .thenReturn(List.of(new LegalClause("3.1", "Liability", "Direct damages only.", "HIGH")));

        mockMvc.perform(post("/api/scenarios/legal/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": "liability"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].section").value("3.1"))
                .andExpect(jsonPath("$[0].relevance").value("HIGH"));
    }

    @Test
    void compliance_returnsPracticeAndAnalysis() throws Exception {
        when(legalSearchService.complianceCheck("sharing user data"))
                .thenReturn(Map.of("practice", "sharing user data", "analysis", "NON-COMPLIANT"));

        mockMvc.perform(post("/api/scenarios/legal/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"practice": "sharing user data"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practice").value("sharing user data"))
                .andExpect(jsonPath("$.analysis").value("NON-COMPLIANT"));
    }

    @Test
    void search_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/legal/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compliance_blankPractice_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/legal/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"practice": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


