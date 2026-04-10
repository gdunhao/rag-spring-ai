# Demo 5: Advisors

## What are Advisors?

**Advisors** in Spring AI follow the **chain-of-responsibility pattern** — they intercept and modify the request/response pipeline, similar to servlet filters or Spring interceptors. They allow you to add cross-cutting behavior without modifying your core logic.

```
User Request → [Advisor 1] → [Advisor 2] → [Advisor N] → LLM Call → Response
                SafeGuard     Memory         RAG
```

## Built-in Advisors

### 1. `QuestionAnswerAdvisor` (RAG)

The core RAG advisor. It:
1. Takes the user's question
2. Searches the VectorStore for relevant documents
3. Augments the prompt with retrieved context
4. The LLM generates an answer grounded in that context

```java
new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())
```

**Customizable search parameters:**

```java
SearchRequest searchRequest = SearchRequest.query(question)
    .withTopK(5)                    // Return top 5 results
    .withSimilarityThreshold(0.7);  // Minimum similarity score

new QuestionAnswerAdvisor(vectorStore, searchRequest)
```

### 2. `SafeGuardAdvisor` (Content Moderation)

Blocks prompts containing banned words before they reach the LLM.

```java
List<String> bannedWords = List.of("hack", "exploit", "injection");
new SafeGuardAdvisor(bannedWords)
```

If a banned word is detected, the advisor throws an exception, preventing the LLM call entirely. This is a simple but effective first layer of content moderation.

### 3. `MessageChatMemoryAdvisor` (Conversation History)

Automatically loads/saves conversation history. See [Demo 4: Chat Memory](04-chat-memory.md).

## Composing Advisors

Advisors execute in the order they're added. Order matters!

```java
client.prompt()
    .advisors(
        new SafeGuardAdvisor(bannedWords),      // 1st: Block bad input
        new MessageChatMemoryAdvisor(memory),    // 2nd: Load memory
        new QuestionAnswerAdvisor(vectorStore, req) // 3rd: Retrieve docs
    )
    .user(question)
    .call()
    .content();
```

### Why Order Matters

```
SafeGuard → if blocked, no LLM call (saves cost!)
Memory    → loads conversation history before RAG
RAG       → retrieves docs using the full context (question + history)
```

## API Endpoints

### `POST /api/advisor/custom-retrieval`
RAG with custom search parameters.

```bash
# Retrieve 5 documents with high similarity
curl -X POST "http://localhost:8080/api/advisor/custom-retrieval?topK=5&threshold=0.7" \
  -H "Content-Type: application/json" \
  -d '{"question": "What embedding models can I use?"}'

# Retrieve only 1 most relevant document
curl -X POST "http://localhost:8080/api/advisor/custom-retrieval?topK=1&threshold=0.8" \
  -H "Content-Type: application/json" \
  -d '{"question": "What is a vector store?"}'
```

### `POST /api/advisor/safeguard`
Test the SafeGuard content moderation.

```bash
# Normal question — passes through
curl -X POST http://localhost:8080/api/advisor/safeguard \
  -H "Content-Type: application/json" \
  -d '{"question": "How does Spring AI handle embeddings?"}'

# Blocked question — contains banned word
curl -X POST http://localhost:8080/api/advisor/safeguard \
  -H "Content-Type: application/json" \
  -d '{"question": "How can I hack the system?"}'
```

### `POST /api/advisor/composed`
Multiple advisors working together.

```bash
curl -X POST http://localhost:8080/api/advisor/composed \
  -H "Content-Type: application/json" \
  -d '{"question": "What LLM providers are supported?"}'
```

## Creating Custom Advisors

You can create your own advisors by implementing `RequestResponseAdvisor`:

```java
public class LoggingAdvisor implements RequestResponseAdvisor {
    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        System.out.println("Question: " + request.userText());
        return request;
    }

    @Override
    public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
        System.out.println("Answer: " + response.getResult().getOutput().getContent());
        return response;
    }
}
```

## Source Files
- Service: [`AdvisorService.java`](../src/main/java/com/example/rag_spring_ai/advisor/AdvisorService.java)
- Controller: [`AdvisorController.java`](../src/main/java/com/example/rag_spring_ai/advisor/AdvisorController.java)

