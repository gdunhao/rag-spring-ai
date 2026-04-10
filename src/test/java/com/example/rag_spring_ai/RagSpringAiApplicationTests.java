package com.example.rag_spring_ai;

import com.example.rag_spring_ai.config.TestAiConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring application context loads correctly using
 * the "test" profile (H2 datasource, stub AI beans — no real infrastructure needed).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestAiConfiguration.class)
class RagSpringAiApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes the context started successfully with all beans wired.
    }
}
