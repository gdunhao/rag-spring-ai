# Real-World Scenario: Legal Document Search

## Business Context

Legal teams spend enormous time searching through contracts, terms of service, and compliance documents. A RAG-powered legal search tool can:

- **Find relevant clauses** using natural language instead of keyword search
- **Summarize complex legal text** in plain English
- **Check compliance** of business practices against legal documents
- **Extract structured data** from unstructured legal prose

> ⚠️ **Important**: AI-powered legal tools should always be used as assistants, not as substitutes for legal professionals. Always verify AI-generated legal analysis.

## Architecture

```
Legal Query
     ↓
┌────────────────────────────────────────┐
│          Legal Search System            │
│                                         │
│  Terms of Service (chunked by section)  │
│           ↓                             │
│  Vector Store (small chunks for         │
│  precise clause matching)               │
│           ↓                             │
│  Specialized prompts per task:          │
│  • Clause search (plain language)       │
│  • Structured extraction (JSON)         │
│  • Compliance analysis (verdict)        │
└────────────────────────────────────────┘
```

## Key Design Decisions

### Small Chunk Sizes for Legal Documents

Legal documents require **smaller chunks** (300-500 tokens) because:
- Individual clauses are the unit of meaning
- Larger chunks mix clauses, reducing precision
- Subsections often have independent legal significance

```java
var splitter = new TokenTextSplitter(400, 50, 5, 100, true);
```

### Specialized System Prompts

Each legal task gets a tailored system prompt:

| Task | Prompt Focus |
|------|-------------|
| Clause search | "Cite section numbers, use plain language" |
| Structured extraction | "Return section, title, summary, relevance" |
| Compliance check | "Give a COMPLIANT/NON-COMPLIANT verdict with reasoning" |

## API Endpoints

### `POST /api/scenarios/legal/search`
Natural language search through legal documents.

```bash
# Search for privacy-related clauses
curl -X POST http://localhost:8080/api/scenarios/legal/search \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the data privacy and encryption provisions?"}'

# Search for liability limits
curl -X POST http://localhost:8080/api/scenarios/legal/search \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the liability limitations?"}'

# Search for termination conditions
curl -X POST http://localhost:8080/api/scenarios/legal/search \
  -H "Content-Type: application/json" \
  -d '{"query": "Under what conditions can my account be terminated?"}'
```

### `POST /api/scenarios/legal/extract`
Extract structured clause information.

```bash
curl -X POST http://localhost:8080/api/scenarios/legal/extract \
  -H "Content-Type: application/json" \
  -d '{"query": "payment and refund policies"}'
```

**Response:**
```json
[
  {
    "section": "6",
    "title": "Payment and Billing",
    "summary": "Plans are billed monthly/annually in advance. Refunds available within 30 days.",
    "relevance": "HIGH"
  },
  {
    "section": "9",
    "title": "Termination",
    "summary": "Account termination leads to loss of access and content.",
    "relevance": "MEDIUM"
  }
]
```

### `POST /api/scenarios/legal/compliance`
Check if a business practice complies with the Terms of Service.

```bash
# Check AI training data usage
curl -X POST http://localhost:8080/api/scenarios/legal/compliance \
  -H "Content-Type: application/json" \
  -d '{"practice": "Using customer uploaded documents to train our AI models without explicit opt-in"}'

# Check data sharing
curl -X POST http://localhost:8080/api/scenarios/legal/compliance \
  -H "Content-Type: application/json" \
  -d '{"practice": "Sharing anonymized usage analytics with third-party advertising partners"}'

# Check account policies
curl -X POST http://localhost:8080/api/scenarios/legal/compliance \
  -H "Content-Type: application/json" \
  -d '{"practice": "Deleting inactive free accounts after 6 months without notice"}'
```

## Real-World Applications

| Industry | Application |
|----------|-------------|
| Legal firms | Contract review and clause comparison |
| Healthcare | HIPAA compliance checking |
| Finance | Regulatory document analysis |
| HR | Employment law compliance |
| Real estate | Lease agreement analysis |

## Source Files
- Service: [`LegalSearchService.java`](../src/main/java/com/example/rag_spring_ai/scenarios/legal/LegalSearchService.java)
- Controller: [`LegalSearchController.java`](../src/main/java/com/example/rag_spring_ai/scenarios/legal/LegalSearchController.java)
- Document: [`terms-of-service.txt`](../src/main/resources/documents/legal/terms-of-service.txt)

