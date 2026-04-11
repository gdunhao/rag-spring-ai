# Demo 4: Chat with Memory

## The Problem

LLMs are **stateless** — each API call is independent. Without memory, the model can't:
- Remember what you asked before
- Understand "tell me more about **that**"
- Build on previous context in a conversation

## How Chat Memory Works

```
Turn 1: User: "What is Spring AI?"
        → LLM sees: [User: "What is Spring AI?"]
        → Response: "Spring AI is a framework..."

Turn 2: User: "What vector stores does it support?"
        → LLM sees: [User: "What is Spring AI?",
                      Assistant: "Spring AI is a framework...",
                      User: "What vector stores does it support?"]
        → Response: "Spring AI supports PgVector, Chroma..." (understands "it" = Spring AI)
```

The `MessageChatMemoryAdvisor` automatically:
1. Loads previous messages from `ChatMemory` before the LLM call
2. Adds them to the prompt
3. Saves the new exchange after the LLM responds

## Spring AI Components

### `InMemoryChatMemoryRepository` + `MessageWindowChatMemory`
Stores conversation history in memory. Simple, but lost on restart.

```java
InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(memoryRepository)
    .build();
```

### `MessageChatMemoryAdvisor`
An advisor that integrates chat memory into the ChatClient pipeline.

```java
ChatClient client = chatClientBuilder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();
```

### Combining Memory + RAG

The power comes from combining memory with RAG:

```java
// Build the client once with the memory advisor as a default
ChatClient client = chatClientBuilder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();

// Pass the session ID at call time via an advisor parameter
client.prompt()
    .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
    .user(message)
    .call()
    .content();
```

Now the system can handle follow-up questions that reference both the conversation history AND the document knowledge base.

## Session Management

Each conversation is identified by a session ID. A single shared `ChatMemory` instance backs all sessions — the session ID is passed at call time as an advisor parameter:

```java
// In the constructor (once):
InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(memoryRepository)
    .build();

// At call time — session ID selects the correct conversation slice:
client.prompt()
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
    .user(message)
    .call()
    .content();
```

## API Endpoints

### `POST /api/chat/{sessionId}`
Chat with RAG + memory. Try a multi-turn conversation:

```bash
# Turn 1: Ask about Spring AI
curl -X POST http://localhost:8080/api/chat/session1 \
  -H "Content-Type: application/json" \
  -d '{"message": "What is Spring AI?"}'

# Turn 2: Follow-up (uses memory to understand "it")
curl -X POST http://localhost:8080/api/chat/session1 \
  -H "Content-Type: application/json" \
  -d '{"message": "What vector stores does it support?"}'

# Turn 3: Another follow-up
curl -X POST http://localhost:8080/api/chat/session1 \
  -H "Content-Type: application/json" \
  -d '{"message": "Which one would you recommend for a small project?"}'
```

### `POST /api/chat/{sessionId}/simple`
Chat with memory only (no RAG). Good for comparing behavior.

```bash
curl -X POST http://localhost:8080/api/chat/session2/simple \
  -H "Content-Type: application/json" \
  -d '{"message": "My name is Alice"}'

curl -X POST http://localhost:8080/api/chat/session2/simple \
  -H "Content-Type: application/json" \
  -d '{"message": "What is my name?"}'
# → Should remember "Alice"
```

### `DELETE /api/chat/{sessionId}`
Clear a session's memory.

```bash
curl -X DELETE http://localhost:8080/api/chat/session1
```

### `GET /api/chat/sessions`
List all active sessions.

```bash
curl http://localhost:8080/api/chat/sessions
```

## Production Considerations

| Aspect | Demo | Production |
|--------|------|------------|
| Memory Store | `MessageWindowChatMemory` + `InMemoryChatMemoryRepository` | Redis, database, or `CassandraChatMemory` |
| Session ID | Manual path param | JWT token or session cookie |
| Memory Limit | `MessageWindowChatMemory` defaults to last 20 messages | Tune `maxMessages` to control cost |
| Persistence | Lost on restart | Persistent store with TTL |

## Source Files
- Service: [`ChatMemoryService.java`](../src/main/java/com/example/rag_spring_ai/memory/ChatMemoryService.java)
- Controller: [`ChatMemoryController.java`](../src/main/java/com/example/rag_spring_ai/memory/ChatMemoryController.java)

