package com.example.rag_spring_ai.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class TestAiConfiguration {

    static final String STUB_RESPONSE = "This is a test response from the stub chat model.";

    @Bean @Primary
    public ChatModel chatModel() {
        return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(STUB_RESPONSE))));
    }

    @Bean @Primary
    public EmbeddingModel embeddingModel() {
        return new StubEmbeddingModel();
    }

    @Bean @Primary
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
