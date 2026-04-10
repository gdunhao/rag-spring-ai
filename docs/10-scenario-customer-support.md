# Real-World Scenario: Customer Support Bot

## Business Context

Every SaaS company faces the same challenge: customers have questions, and human support agents are expensive and limited in availability. A **RAG-powered support bot** can:

- Answer 60-80% of common questions instantly (24/7)
- Reduce support ticket volume and response times
- Maintain consistent, accurate answers from the official FAQ
- Escalate complex issues to human agents with full context

## Architecture

```
Customer Message
       ↓
┌─────────────────────────────────────────┐
│              Support Bot                 │
│                                          │
│  1. Chat Memory (conversation context)   │
│  2. FAQ Vector Store (RAG retrieval)     │
│  3. Function Calling (ticket creation)   │
│  4. System Prompt (behavior guidelines)  │
│                                          │
│  Can answer from FAQ? ──Yes──▶ Answer    │
│         │                                │
│         No                               │
│         ↓                                │
│  Create support ticket via function call │
└─────────────────────────────────────────┘
```

## Spring AI Features Used

| Feature | How It's Used |
|---------|---------------|
| `QuestionAnswerAdvisor` | Retrieves relevant FAQ entries for each question |
| `MessageChatMemoryAdvisor` | Remembers the conversation across messages |
| Function Calling | Creates support tickets, looks up orders |
| System Prompt | Defines the bot's personality and behavior rules |

## The System Prompt

The system prompt is critical for support bots — it defines behavior:

```
You are a friendly and professional customer support agent for CloudFlow.
Your responsibilities:
1. Answer questions using the FAQ knowledge base
2. Be empathetic with frustrated customers
3. Provide specific, actionable answers
4. If you cannot answer, create a support ticket
5. Always sign off as "CloudFlow Support Team"
```

## Example Conversations

### Conversation 1: Answerable from FAQ

```bash
# Customer asks about pricing
curl -X POST http://localhost:8080/api/scenarios/support/customer1 \
  -H "Content-Type: application/json" \
  -d '{"message": "What pricing plans do you offer?"}'

# Follow-up question (memory remembers context)
curl -X POST http://localhost:8080/api/scenarios/support/customer1 \
  -H "Content-Type: application/json" \
  -d '{"message": "Can I try the Professional plan for free?"}'
```

### Conversation 2: Needs Escalation

```bash
# Issue not in FAQ → bot creates a ticket
curl -X POST http://localhost:8080/api/scenarios/support/customer2 \
  -H "Content-Type: application/json" \
  -d '{"message": "My data exports have been failing for the past 3 days and I am losing business"}'
```

### Conversation 3: Order Inquiry

```bash
# Customer asks about their order → bot calls lookupOrder function
curl -X POST http://localhost:8080/api/scenarios/support/customer3 \
  -H "Content-Type: application/json" \
  -d '{"message": "Can you check the status of my order ORD-001?"}'
```

### End Session

```bash
curl -X DELETE http://localhost:8080/api/scenarios/support/customer1
```

## Production Enhancements

For a real deployment, you would add:

1. **User authentication** — identify the customer for personalized responses
2. **Persistent memory** — Redis or database-backed chat memory
3. **Real ticketing integration** — Jira, Zendesk, Intercom API
4. **Analytics** — track resolution rates, common questions, escalation reasons
5. **Feedback loop** — let customers rate answers to improve the FAQ
6. **Fallback handling** — graceful degradation when the LLM is slow or unavailable
7. **Multi-language support** — detect language and respond accordingly

## Source Files
- Service: [`CustomerSupportService.java`](../src/main/java/com/example/rag_spring_ai/scenarios/support/CustomerSupportService.java)
- Controller: [`CustomerSupportController.java`](../src/main/java/com/example/rag_spring_ai/scenarios/support/CustomerSupportController.java)
- FAQ Data: [`customer-faq.txt`](../src/main/resources/documents/faq/customer-faq.txt)

