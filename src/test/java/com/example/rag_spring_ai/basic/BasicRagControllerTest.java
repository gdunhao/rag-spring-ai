package com.example.rag_spring_ai.basic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BasicRagController.class)
@ActiveProfiles("test")
class BasicRagControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean BasicRagService ragService;

    @Test
    void ask_returnsQuestionAndAnswer() throws Exception {
        when(ragService.ask("What is Spring AI?")).thenReturn("Spring AI is a framework.");

        mockMvc.perform(post("/api/basic/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What is Spring AI?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is Spring AI?"))
                .andExpect(jsonPath("$.answer").value("Spring AI is a framework."));
    }

    @Test
    void ask_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/basic/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingest_returnsSuccessStatus() throws Exception {
        doNothing().when(ragService).ingestDocuments();

        mockMvc.perform(post("/api/basic/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Documents ingested successfully"));

        verify(ragService, times(1)).ingestDocuments();
    }
}


