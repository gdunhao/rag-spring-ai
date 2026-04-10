package com.example.rag_spring_ai.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatMemoryController.class)
@ActiveProfiles("test")
class ChatMemoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ChatMemoryService chatMemoryService;

    @Test
    void chat_returnsSessionIdMessageAndResponse() throws Exception {
        when(chatMemoryService.chat("sess1", "Hello")).thenReturn("Hi there!");

        mockMvc.perform(post("/api/chat/sess1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess1"))
                .andExpect(jsonPath("$.message").value("Hello"))
                .andExpect(jsonPath("$.response").value("Hi there!"));
    }

    @Test
    void chatSimple_returnsSessionIdAndResponse() throws Exception {
        when(chatMemoryService.chatWithoutRag("sess2", "Simple msg")).thenReturn("Simple reply");

        mockMvc.perform(post("/api/chat/sess2/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Simple msg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess2"))
                .andExpect(jsonPath("$.response").value("Simple reply"));
    }

    @Test
    void clearSession_returnsStatusAndSessionId() throws Exception {
        doNothing().when(chatMemoryService).clearSession("sess3");

        mockMvc.perform(delete("/api/chat/sess3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Session cleared"))
                .andExpect(jsonPath("$.sessionId").value("sess3"));

        verify(chatMemoryService).clearSession("sess3");
    }

    @Test
    void sessions_returnsSessionInfo() throws Exception {
        when(chatMemoryService.getSessionInfo())
                .thenReturn(Map.of("activeSessions", 2, "sessionIds", Set.of("s1", "s2")));

        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSessions").value(2));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat/sess1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


