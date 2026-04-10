package com.example.rag_spring_ai.function;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Service demonstrating Function Calling (Tool Use) combined with RAG.
 *
 * In Spring AI 1.0.0, tools are defined as methods annotated with {@code @Tool}
 * inside a plain object. Pass the object instance to {@code .tools(toolObject)}
 * on the ChatClient prompt — Spring AI introspects it automatically.
 *
 * NOTE: {@link ChatClient} is built once and reused; the system prompt is set
 * per-call via {@code .system()} since the two methods have distinct personas.
 */
@Service
public class FunctionCallingService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final FunctionConfig.SupportTools supportTools;
    private final FunctionConfig.WeatherTools weatherTools;

    public FunctionCallingService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            FunctionConfig.SupportTools supportTools,
            FunctionConfig.WeatherTools weatherTools) {
        this.vectorStore = vectorStore;
        this.supportTools = supportTools;
        this.weatherTools = weatherTools;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * LLM decides whether to create a support ticket based on user's issue.
     * Uses RAG to check if the issue is covered in the FAQ first.
     */
    public String handleSupportRequest(String userMessage) {
        return chatClient.prompt()
                .system("""
                        You are a customer support agent for CloudFlow. First, check the knowledge
                        base for relevant information. If you can answer the question directly from
                        the FAQ or documentation, do so. If the issue requires human intervention
                        or is not covered in the knowledge base, use the createTicket tool to
                        create a support ticket.
                        """)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .tools(supportTools)
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * LLM uses weather and support tools alongside RAG context.
     */
    public String askWithTools(String question) {
        return chatClient.prompt()
                .system("""
                        You are a helpful assistant with access to tools. Use the available
                        tools when the user asks about weather or order status. For other
                        questions, use your knowledge base context.
                        """)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .tools(weatherTools, supportTools)
                .user(question)
                .call()
                .content();
    }
}
