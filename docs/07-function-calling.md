# Demo 7: Function Calling (Tool Use)

## What is Function Calling?

**Function calling** (also called **tool use**) lets the LLM decide when to invoke application-defined functions during its reasoning process. Instead of just generating text, the LLM can:

1. Recognize that it needs to perform an action (e.g., create a ticket, look up data)
2. Generate a structured function call request with the correct arguments
3. Receive the function result and incorporate it into its response

```
User: "What's the status of order ORD-001?"

LLM thinks: "I need to look up an order. I'll call the lookupOrder function."
     ↓
LLM generates: lookupOrder({ orderId: "ORD-001" })
     ↓
Spring AI executes the function → returns OrderResponse
     ↓
LLM receives result → generates human-readable response:
"Your order ORD-001 has been shipped and is estimated to arrive on April 12, 2026."
```

## How It Works in Spring AI 1.0.0

### 1. Define Tools as `@Tool`-annotated Methods

Create a plain class whose methods are annotated with `@Tool`. The `description` tells the LLM *when* to invoke the method:

```java
public static class SupportTools {

    @Tool(description = "Look up the status of a customer order by order ID.")
    public OrderResponse lookupOrder(OrderRequest request) {
        // ... implementation
    }

    @Tool(description = "Create a customer support ticket when the issue needs human intervention.")
    public TicketResponse createTicket(TicketRequest request) {
        // ... implementation
    }
}

// Register as a Spring bean so it can be injected elsewhere
@Bean
public SupportTools supportTools() { return new SupportTools(); }
```

### 2. Pass the Tool Object to the ChatClient

```java
// Inject the bean
@Autowired SupportTools supportTools;

// Pass the instance — Spring AI introspects @Tool methods automatically
client.prompt()
    .tools(supportTools)          // ← object instance, not string name
    .user(question)
    .call()
    .content();
```

### 3. Spring AI Handles the Rest

Under the hood, Spring AI:
- Generates a JSON schema from each `@Tool`-annotated method's parameter type
- Sends the tool schemas alongside the prompt to the LLM
- Parses the LLM's tool-call decisions
- Deserializes arguments and invokes the method
- Returns the result to the LLM for final response generation

> **Migration note** — Spring AI 1.0.0 removed the old `@Bean @Description Function<Input, Output>` bean pattern.
> Strings like `.tools("beanName")` no longer work; you must pass an object instance whose methods carry `@Tool`.

## Tools in This Demo

| Method | Input | Output | Use Case |
|--------|-------|--------|----------|
| `SupportTools.createTicket` | name, issue, priority | ticketId, status, message | Support escalation |
| `SupportTools.lookupOrder` | orderId | status, delivery date, items | Order tracking |
| `WeatherTools.getWeather` | city | temperature, condition | General utility |

## Combining Function Calling with RAG

This is where things get powerful. The LLM can:

1. **First** retrieve relevant context via RAG (`QuestionAnswerAdvisor`)
2. **Then** decide if it can answer from context alone, or needs to call a tool

```java
client.prompt()
    .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())  // RAG
    .tools(supportTools)                                            // Tool use
    .user("My account is locked and I need help")
    .call()
    .content();
```

The LLM's decision flow:
```
1. Check FAQ context → "Account issues" → found some info
2. But the user's specific issue isn't covered → decide to create a ticket
3. Call createTicket({ issue: "Account locked", priority: "HIGH" })
4. Respond: "I've created ticket TKT-A1B2C3D4 for your account issue..."
```

## API Endpoints

### `POST /api/function/support`
Handle a support request — LLM decides whether to answer from FAQ or create a ticket.

```bash
# Answerable from FAQ
curl -X POST http://localhost:8080/api/function/support \
  -H "Content-Type: application/json" \
  -d '{"message": "What are your pricing plans?"}'

# Needs ticket creation
curl -X POST http://localhost:8080/api/function/support \
  -H "Content-Type: application/json" \
  -d '{"message": "My account has been charged twice this month and I need a refund immediately"}'
```

### `POST /api/function/ask`
Ask with multiple tools available.

```bash
# Weather query → calls getWeather tool
curl -X POST http://localhost:8080/api/function/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the weather like in Tokyo?"}'

# Order tracking → calls lookupOrder tool
curl -X POST http://localhost:8080/api/function/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the status of order ORD-001?"}'

# Knowledge question → uses RAG context (no tool call)
curl -X POST http://localhost:8080/api/function/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What vector stores does Spring AI support?"}'
```

## Important Notes

- **Model support**: Not all models support function calling well. Qwen3 4B has decent tool-use support; larger models (8B+) are more reliable and faster at deciding when to call tools.
- **Latency**: Tool calling requires multiple LLM round trips. With qwen3:4b on CPU expect **60–180 s** per request. Set `spring.ai.ollama.client.read-timeout` appropriately (default in this project: 5 minutes).
- **Determinism**: The LLM decides *when* to call tools — it may answer from context instead of calling a tool, depending on prompt wording.
- **Safety**: Always validate tool inputs. The LLM controls the arguments passed to your methods!

## Source Files
- Service: [`FunctionCallingService.java`](../src/main/java/com/example/rag_spring_ai/function/FunctionCallingService.java)
- Config: [`FunctionConfig.java`](../src/main/java/com/example/rag_spring_ai/function/FunctionConfig.java)
- Controller: [`FunctionCallingController.java`](../src/main/java/com/example/rag_spring_ai/function/FunctionCallingController.java)
