# Real-World Scenario: HR Policy Q&A

## Business Context

HR departments spend a disproportionate amount of time answering the same policy questions:

- "How many vacation days do I get?"
- "What's the parental leave policy?"
- "How does the 401(k) match work?"
- "What's the expense reimbursement process?"

An HR Policy Q&A bot powered by RAG provides:
- **Instant, 24/7 answers** to common policy questions
- **Consistent information** — everyone gets the same accurate answer
- **Reduced HR workload** — agents focus on complex, unique cases
- **Conversational follow-ups** — employees can ask clarifying questions

## Architecture

```
Employee Question
       ↓
┌──────────────────────────────────────────┐
│           HR Policy Assistant             │
│                                           │
│  HR Handbook (chunked by section)         │
│           ↓                               │
│  Vector Store                             │
│           ↓                               │
│  Three modes:                             │
│  • Chat (memory + RAG for conversations)  │
│  • Structured (PolicyInfo extraction)     │
│  • Quick (single-turn, no memory)         │
│                                           │
│  Empathetic, section-citing responses     │
└──────────────────────────────────────────┘
```

## Key Design Decisions

### Empathetic System Prompt

HR questions often involve sensitive topics. The system prompt includes:
```
Be empathetic... If a question involves a sensitive topic (termination, PIP, etc.),
suggest contacting HR directly... For complex scenarios, explain the general policy
first, then note that individual circumstances may vary.
```

### Section Citations

Every answer cites the relevant policy section, so employees can verify:
```
"According to Section 3.1, new employees (years 1-3) receive 20 PTO days per year..."
```

### Three Interaction Modes

| Mode | Use Case | Has Memory |
|------|----------|------------|
| Chat | Multi-turn conversations | ✅ Yes |
| Structured | Policy extraction for dashboards | ❌ No |
| Quick | Single question, fast answer | ❌ No |

## API Endpoints

### `POST /api/scenarios/hr/chat/{sessionId}`
Conversational HR Q&A with session memory.

```bash
# Ask about PTO
curl -X POST http://localhost:8080/api/scenarios/hr/chat/emp1 \
  -H "Content-Type: application/json" \
  -d '{"question": "How many vacation days do I get as a new employee?"}'

# Follow-up (memory remembers the context)
curl -X POST http://localhost:8080/api/scenarios/hr/chat/emp1 \
  -H "Content-Type: application/json" \
  -d '{"question": "What about after 5 years?"}'

# Different topic, same session
curl -X POST http://localhost:8080/api/scenarios/hr/chat/emp1 \
  -H "Content-Type: application/json" \
  -d '{"question": "Can I work remotely from another country?"}'
```

### `POST /api/scenarios/hr/policy`
Get structured policy information — great for dashboards or comparison tables.

```bash
# Parental leave policy
curl -X POST http://localhost:8080/api/scenarios/hr/policy \
  -H "Content-Type: application/json" \
  -d '{"topic": "parental leave"}'

# 401(k) retirement
curl -X POST http://localhost:8080/api/scenarios/hr/policy \
  -H "Content-Type: application/json" \
  -d '{"topic": "retirement and 401k"}'

# Remote work
curl -X POST http://localhost:8080/api/scenarios/hr/policy \
  -H "Content-Type: application/json" \
  -d '{"topic": "remote work and home office"}'
```

**Response:**
```json
{
  "policyName": "Parental Leave",
  "summary": "CloudFlow offers paid parental leave for both primary and secondary caregivers.",
  "eligibility": "All full-time employees after probation period",
  "keyPoints": "Primary caregivers: 16 weeks paid, Secondary caregivers: 8 weeks paid, Must be taken within 12 months, Part-time return available"
}
```

### `POST /api/scenarios/hr/quick`
Quick single-turn answers — no session overhead.

```bash
# Health insurance
curl -X POST http://localhost:8080/api/scenarios/hr/quick \
  -H "Content-Type: application/json" \
  -d '{"question": "What health insurance options are available?"}'

# Performance reviews
curl -X POST http://localhost:8080/api/scenarios/hr/quick \
  -H "Content-Type: application/json" \
  -d '{"question": "When are performance reviews conducted?"}'

# Expense policy
curl -X POST http://localhost:8080/api/scenarios/hr/quick \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the daily meal limit for business travel?"}'

# Resignation
curl -X POST http://localhost:8080/api/scenarios/hr/quick \
  -H "Content-Type: application/json" \
  -d '{"question": "How much notice do I need to give if I resign?"}'
```

## Accuracy Considerations

HR Q&A systems require high accuracy. Strategies:

1. **Ground in official documents** — only answer from the ingested handbook
2. **Cite sections** — employees can verify against the source
3. **Admit uncertainty** — "I'd recommend checking with HR directly"
4. **Regular updates** — re-ingest when policies change
5. **Human review** — flag low-confidence answers for HR review

## Production Enhancements

- **Slack/Teams integration** — employees ask in their chat tool
- **Policy change notifications** — alert when policies they asked about change
- **Analytics dashboard** — most common questions, unanswered topics
- **Multilingual support** — serve a global workforce
- **Role-based filtering** — different policies for different employee levels

## Source Files
- Service: [`HrPolicyService.java`](../src/main/java/com/example/rag_spring_ai/scenarios/hr/HrPolicyService.java)
- Controller: [`HrPolicyController.java`](../src/main/java/com/example/rag_spring_ai/scenarios/hr/HrPolicyController.java)
- Document: [`hr-policies.txt`](../src/main/resources/documents/hr/hr-policies.txt)

