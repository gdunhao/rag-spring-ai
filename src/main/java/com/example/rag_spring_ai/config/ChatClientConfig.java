package com.example.rag_spring_ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the default {@link ChatClient}.
 *
 * The {@link ChatClient.Builder} is auto-configured by Spring AI's Ollama starter.
 * We create a single shared ChatClient bean with a sensible default system prompt.
 * Individual demos may create their own ChatClient instances with custom prompts.
 *
 * {@link SimpleLoggerAdvisor} is added as a default advisor so every LLM call
 * is logged at DEBUG level — enable with:
 *   logging.level.org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor: DEBUG
 */
@Configuration
public class ChatClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatClientConfig.class);

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        log.info("[→Ollama] Configuring shared ChatClient | model=qwen3:4b | advisor=SimpleLoggerAdvisor");
        return builder
                .defaultSystem("""
                        You are a helpful AI assistant. When answering questions, use the
                        provided context from the retrieved documents. If you don't know
                        the answer or the context doesn't contain relevant information,
                        say so clearly. Always be concise and factual.
                        """)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
