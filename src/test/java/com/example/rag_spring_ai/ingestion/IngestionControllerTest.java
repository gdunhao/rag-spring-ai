package com.example.rag_spring_ai.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngestionController.class)
@ActiveProfiles("test")
class IngestionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IngestionService ingestionService;

    @Test
    void ingestText_returnsServiceResult() throws Exception {
        when(ingestionService.ingestText()).thenReturn(
                Map.of("status", "ingested", "chunksCreated", 5));

        mockMvc.perform(post("/api/ingest/text"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ingested"))
                .andExpect(jsonPath("$.chunksCreated").value(5));
    }

    @Test
    void ingestJson_returnsServiceResult() throws Exception {
        when(ingestionService.ingestJson()).thenReturn(
                Map.of("status", "ingested", "format", "json"));

        mockMvc.perform(post("/api/ingest/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("json"));
    }

    @Test
    void ingestCustomChunking_defaultParams_callsServiceWithDefaults() throws Exception {
        when(ingestionService.ingestWithCustomChunking(400, 50)).thenReturn(
                Map.of("status", "ingested", "chunkSize", 400));

        mockMvc.perform(post("/api/ingest/custom-chunking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkSize").value(400));

        verify(ingestionService).ingestWithCustomChunking(400, 50);
    }

    @Test
    void ingestCustomChunking_customParams_callsServiceWithProvidedValues() throws Exception {
        when(ingestionService.ingestWithCustomChunking(300, 30)).thenReturn(
                Map.of("status", "ingested", "chunkSize", 300));

        mockMvc.perform(post("/api/ingest/custom-chunking?chunkSize=300&minChunkSize=30"))
                .andExpect(status().isOk());

        verify(ingestionService).ingestWithCustomChunking(300, 30);
    }
}


