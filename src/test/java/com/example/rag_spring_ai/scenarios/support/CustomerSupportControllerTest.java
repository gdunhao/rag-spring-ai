package com.example.rag_spring_ai.scenarios.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerSupportController.class)
@ActiveProfiles("test")
class CustomerSupportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerSupportService supportService;

    @Test
    void chat_returnsSessionMessageAndResponse() throws Exception {
        when(supportService.handleMessage("session1", "I can't log in"))
                .thenReturn(Map.of(
                        "sessionId", "session1",
                        "message", "I can't log in",
                        "response", "Please reset your password."
                ));

        mockMvc.perform(post("/api/scenarios/support/session1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "I can't log in"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session1"))
                .andExpect(jsonPath("$.message").value("I can't log in"))
                .andExpect(jsonPath("$.response").value("Please reset your password."));
    }

    @Test
    void endSession_returnsStatusAndSessionId() throws Exception {
        doNothing().when(supportService).endSession("session1");

        mockMvc.perform(delete("/api/scenarios/support/session1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Session ended"))
                .andExpect(jsonPath("$.sessionId").value("session1"));

        verify(supportService).endSession("session1");
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/support/session1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


