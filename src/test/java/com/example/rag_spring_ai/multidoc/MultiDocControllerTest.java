package com.example.rag_spring_ai.multidoc;

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

@WebMvcTest(MultiDocController.class)
@ActiveProfiles("test")
class MultiDocControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MultiDocService multiDocService;

    @Test
    void listCollections_returnsAllFourCollections() throws Exception {
        when(multiDocService.listCollections()).thenReturn(List.of(
                Map.of("name", "faq"),
                Map.of("name", "legal"),
                Map.of("name", "tech"),
                Map.of("name", "hr")
        ));

        mockMvc.perform(get("/api/multidoc/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void queryCollection_returnsCollectionQuestionAndAnswer() throws Exception {
        when(multiDocService.queryCollection("faq", "What plans are available?"))
                .thenReturn("We have Starter, Pro, and Enterprise plans.");

        mockMvc.perform(post("/api/multidoc/query/faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What plans are available?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").value("faq"))
                .andExpect(jsonPath("$.question").value("What plans are available?"))
                .andExpect(jsonPath("$.answer").value("We have Starter, Pro, and Enterprise plans."));
    }

    @Test
    void smartQuery_returnsDetectedCollectionAndAnswer() throws Exception {
        when(multiDocService.smartQuery("What are the pricing plans?"))
                .thenReturn(Map.of(
                        "question", "What are the pricing plans?",
                        "detectedCollection", "faq",
                        "answer", "We have Starter, Pro, and Enterprise."
                ));

        mockMvc.perform(post("/api/multidoc/smart-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What are the pricing plans?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedCollection").value("faq"))
                .andExpect(jsonPath("$.answer").value("We have Starter, Pro, and Enterprise."));
    }

    @Test
    void queryCollection_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/multidoc/query/faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


