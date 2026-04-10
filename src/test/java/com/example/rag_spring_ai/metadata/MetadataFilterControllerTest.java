package com.example.rag_spring_ai.metadata;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetadataFilterController.class)
@ActiveProfiles("test")
class MetadataFilterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MetadataFilterService metadataFilterService;

    @Test
    void searchByProduct_defaultProduct_callsServiceWithCloudflow() throws Exception {
        when(metadataFilterService.searchByProduct("rate limits", "cloudflow"))
                .thenReturn(List.of(Map.of("content", "CloudFlow API rate limits", "metadata", Map.of())));

        mockMvc.perform(get("/api/metadata/search/product?query=rate limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("CloudFlow API rate limits"));

        verify(metadataFilterService).searchByProduct("rate limits", "cloudflow");
    }

    @Test
    void searchByProduct_customProduct_callsServiceWithThatProduct() throws Exception {
        when(metadataFilterService.searchByProduct("sync", "datasync")).thenReturn(List.of());

        mockMvc.perform(get("/api/metadata/search/product?query=sync&product=datasync"))
                .andExpect(status().isOk());

        verify(metadataFilterService).searchByProduct("sync", "datasync");
    }

    @Test
    void searchByCategory_defaultCategory_callsServiceWithReleaseNotes() throws Exception {
        when(metadataFilterService.searchByCategory("new features", "release-notes"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/metadata/search/category?query=new features"))
                .andExpect(status().isOk());

        verify(metadataFilterService).searchByCategory("new features", "release-notes");
    }

    @Test
    void askAboutProduct_returnsAnswerWithProductAndQuestion() throws Exception {
        when(metadataFilterService.askAboutProduct("What are rate limits?", "cloudflow"))
                .thenReturn("Rate limits are: ...");

        mockMvc.perform(post("/api/metadata/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What are rate limits?", "product": "cloudflow"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What are rate limits?"))
                .andExpect(jsonPath("$.product").value("cloudflow"))
                .andExpect(jsonPath("$.answer").value("Rate limits are: ..."));
    }

    @Test
    void askAboutProduct_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/metadata/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "", "product": "cloudflow"}
                                """))
                .andExpect(status().isBadRequest());
    }
}


