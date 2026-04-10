package com.example.rag_spring_ai.function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FunctionCallingController.class)
@ActiveProfiles("test")
class FunctionCallingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FunctionCallingService functionCallingService;

    @Test
    void support_returnsMessageAndResponse() throws Exception {
        when(functionCallingService.handleSupportRequest("Login is broken"))
                .thenReturn("Ticket TKT-001 created.");

        mockMvc.perform(post("/api/function/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Login is broken"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login is broken"))
                .andExpect(jsonPath("$.response").value("Ticket TKT-001 created."));
    }

    @Test
    void ask_returnsQuestionAndAnswer() throws Exception {
        when(functionCallingService.askWithTools("What is the weather in Tokyo?"))
                .thenReturn("It's sunny and 22°C in Tokyo.");

        mockMvc.perform(post("/api/function/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What is the weather in Tokyo?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is the weather in Tokyo?"))
                .andExpect(jsonPath("$.answer").value("It's sunny and 22°C in Tokyo."));
    }

    @Test
    void support_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/function/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ask_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/function/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


