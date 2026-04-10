package com.example.rag_spring_ai.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VectorStoreController.class)
@ActiveProfiles("test")
class VectorStoreControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean VectorStoreService vectorStoreService;

    @Test
    void addSamples_returnsServiceResult() throws Exception {
        when(vectorStoreService.addSampleDocuments())
                .thenReturn(Map.of("documentsAdded", 6, "status", "success"));

        mockMvc.perform(post("/api/vectorstore/add-samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentsAdded").value(6))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void search_defaultTopK_callsServiceWith3() throws Exception {
        when(vectorStoreService.search("Java", 3)).thenReturn(List.of(
                Map.of("content", "Java is OOP", "metadata", Map.of(), "id", "1")
        ));

        mockMvc.perform(get("/api/vectorstore/search?query=Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Java is OOP"));

        verify(vectorStoreService).search("Java", 3);
    }

    @Test
    void search_customTopK_callsServiceWithThatValue() throws Exception {
        when(vectorStoreService.search("Python", 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/vectorstore/search?query=Python&topK=5"))
                .andExpect(status().isOk());

        verify(vectorStoreService).search("Python", 5);
    }

    @Test
    void searchWithThreshold_defaultThreshold_callsServiceWith07() throws Exception {
        when(vectorStoreService.searchWithThreshold("Spring", 0.7)).thenReturn(List.of());

        mockMvc.perform(get("/api/vectorstore/search-threshold?query=Spring"))
                .andExpect(status().isOk());

        verify(vectorStoreService).searchWithThreshold("Spring", 0.7);
    }

    @Test
    void embeddingInfo_returnsInfoMap() throws Exception {
        when(vectorStoreService.getEmbeddingInfo("Hello world"))
                .thenReturn(Map.of("text", "Hello world", "dimensions", 768));

        mockMvc.perform(get("/api/vectorstore/embedding-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions").value(768));

        verify(vectorStoreService).getEmbeddingInfo("Hello world");
    }
}


