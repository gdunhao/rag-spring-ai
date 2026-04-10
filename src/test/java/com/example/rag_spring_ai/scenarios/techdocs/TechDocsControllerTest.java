package com.example.rag_spring_ai.scenarios.techdocs;

import com.example.rag_spring_ai.model.ApiEndpoint;
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

@WebMvcTest(TechDocsController.class)
@ActiveProfiles("test")
class TechDocsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TechDocsService techDocsService;

    @Test
    void ask_returnsQuestionAndAnswer() throws Exception {
        when(techDocsService.askTechQuestion("How do I authenticate?"))
                .thenReturn("Use Bearer token in Authorization header.");

        mockMvc.perform(post("/api/scenarios/techdocs/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "How do I authenticate?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("How do I authenticate?"))
                .andExpect(jsonPath("$.answer").value("Use Bearer token in Authorization header."));
    }

    @Test
    void findEndpoints_returnsListOfEndpoints() throws Exception {
        when(techDocsService.findEndpoints("authentication"))
                .thenReturn(List.of(
                        new ApiEndpoint("POST", "/api/auth/token", "Get access token", "username, password")
                ));

        mockMvc.perform(post("/api/scenarios/techdocs/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature": "authentication"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].method").value("POST"))
                .andExpect(jsonPath("$[0].path").value("/api/auth/token"));
    }

    @Test
    void generateCurl_returnsOperationAndCurlExample() throws Exception {
        when(techDocsService.generateCurlExample("list documents"))
                .thenReturn("curl -H 'Authorization: Bearer TOKEN' https://api.cloudflow.io/documents");

        mockMvc.perform(post("/api/scenarios/techdocs/curl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": "list documents"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("list documents"))
                .andExpect(jsonPath("$.curl").exists());
    }

    @Test
    void ask_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/techdocs/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findEndpoints_blankFeature_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/techdocs/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feature": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateCurl_blankOperation_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/techdocs/curl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


