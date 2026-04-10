package com.example.rag_spring_ai.scenarios.hr;

import com.example.rag_spring_ai.model.PolicyInfo;
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

@WebMvcTest(HrPolicyController.class)
@ActiveProfiles("test")
class HrPolicyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HrPolicyService hrPolicyService;

    @Test
    void chat_returnsSessionQuestionAndAnswer() throws Exception {
        when(hrPolicyService.askHr("emp1", "How many PTO days do I get?"))
                .thenReturn(Map.of(
                        "sessionId", "emp1",
                        "question", "How many PTO days do I get?",
                        "answer", "You get 15 days per year."
                ));

        mockMvc.perform(post("/api/scenarios/hr/chat/emp1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "How many PTO days do I get?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("emp1"))
                .andExpect(jsonPath("$.answer").value("You get 15 days per year."));
    }

    @Test
    void getPolicy_returnsStructuredPolicyInfo() throws Exception {
        when(hrPolicyService.getPolicyInfo("PTO"))
                .thenReturn(new PolicyInfo("PTO Policy", "Annual leave.", "All employees", "15 days"));

        mockMvc.perform(post("/api/scenarios/hr/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic": "PTO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyName").value("PTO Policy"))
                .andExpect(jsonPath("$.summary").value("Annual leave."));
    }

    @Test
    void quickAnswer_returnsQuestionAndAnswer() throws Exception {
        when(hrPolicyService.quickAnswer("What is remote work policy?"))
                .thenReturn("Remote work is allowed 3 days per week.");

        mockMvc.perform(post("/api/scenarios/hr/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "What is remote work policy?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is remote work policy?"))
                .andExpect(jsonPath("$.answer").value("Remote work is allowed 3 days per week."));
    }

    @Test
    void getPolicy_blankTopic_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/hr/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quickAnswer_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios/hr/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}


