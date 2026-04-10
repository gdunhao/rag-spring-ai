package com.example.rag_spring_ai.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdvisorController.class)
@ActiveProfiles("test")
class AdvisorControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdvisorService advisorService;

    @Test
    void customRetrieval_returnsAnswerWithDefaultParams() throws Exception {
        when(advisorService.askWithCustomRetrieval("What is RAG?", 3, 0.5)).thenReturn("RAG answer");

        mockMvc.perform(post("/api/advisor/custom-retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What is RAG?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is RAG?"))
                .andExpect(jsonPath("$.answer").value("RAG answer"));
    }

    @Test
    void customRetrieval_respectsRequestParams() throws Exception {
        when(advisorService.askWithCustomRetrieval("query", 5, 0.8)).thenReturn("filtered answer");

        mockMvc.perform(post("/api/advisor/custom-retrieval?topK=5&threshold=0.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "query"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("filtered answer"));
    }

    @Test
    void safeguard_cleanQuestion_returnsFalseBlocked() throws Exception {
        when(advisorService.askWithSafeGuard("safe question"))
                .thenReturn(Map.of("question", "safe question", "answer", "ok", "blocked", "false"));

        mockMvc.perform(post("/api/advisor/safeguard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "safe question"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value("false"));
    }

    @Test
    void composed_returnsAnswer() throws Exception {
        when(advisorService.askWithComposedAdvisors("compose test")).thenReturn("composed answer");

        mockMvc.perform(post("/api/advisor/composed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "compose test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("composed answer"));
    }

    @Test
    void safeguard_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/advisor/safeguard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "  "}
                                """))
                .andExpect(status().isBadRequest());
    }
}


