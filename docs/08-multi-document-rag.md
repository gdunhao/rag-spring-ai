# Demo 8: Multi-Document RAG

## The Problem

Real applications don't have just one document — they have entire collections:
- Customer FAQ for support
- Legal agreements for compliance
- API docs for developers
- HR policies for employees

Dumping everything into a single vector store causes **context pollution** — the LLM gets irrelevant documents mixed with relevant ones.

## Solution: Separate Collections

Create separate `VectorStore` instances for each document domain and **route queries** to the right collection.

```
               ┌──── FAQ Store ──────┐
               │  customer-faq.txt   │
               └─────────────────────┘
               ┌──── Legal Store ────┐
User Query ──▶ │  terms-of-service   │ ──▶ Route to best store ──▶ LLM
               └─────────────────────┘
               ┌──── Tech Store ─────┐
               │  api-guide.txt      │
               └─────────────────────┘
               ┌──── HR Store ───────┐
               │  hr-policies.txt    │
               └─────────────────────┘
```

## Implementation

### Creating Separate Stores

```java
// Each collection gets its own SimpleVectorStore
private VectorStore createAndIngest(Resource resource, String source, String collection) {
    SimpleVectorStore store = new SimpleVectorStore(embeddingModel);
    var reader = new TextReader(resource);
    reader.getCustomMetadata().put("source", source);
    reader.getCustomMetadata().put("collection", collection);
    List<Document> chunks = new TokenTextSplitter().apply(reader.get());
    store.add(chunks);
    return store;
}
```

### Targeted Queries

```java
VectorStore store = switch (collection) {
    case "faq"   -> faqStore;
    case "legal" -> legalStore;
    case "tech"  -> techStore;
    case "hr"    -> hrStore;
    default -> throw new IllegalArgumentException("Unknown: " + collection);
};

client.prompt()
    .advisors(new QuestionAnswerAdvisor(store, SearchRequest.defaults()))
    .user(question)
    .call()
    .content();
```

### Smart Routing (Keyword Heuristic)

A simple approach: detect keywords in the question to choose the collection:

```java
if (question.contains("price") || question.contains("billing")) → faq
if (question.contains("terms") || question.contains("liability")) → legal
if (question.contains("api") || question.contains("endpoint")) → tech
if (question.contains("pto") || question.contains("benefits")) → hr
```

### Advanced Routing (LLM-Based)

For production, you could use the LLM itself to classify the query:

```java
String collection = classifierClient.prompt()
    .user("Classify this question into one of: faq, legal, tech, hr\n" + question)
    .call()
    .content();  // Returns "faq", "legal", etc.
```

## API Endpoints

### `GET /api/multidoc/collections`
List all available document collections.

```bash
curl http://localhost:8080/api/multidoc/collections
```

### `POST /api/multidoc/query/{collection}`
Query a specific collection.

```bash
# Query FAQ
curl -X POST http://localhost:8080/api/multidoc/query/faq \
  -H "Content-Type: application/json" \
  -d '{"question": "What pricing plans are available?"}'

# Query Legal
curl -X POST http://localhost:8080/api/multidoc/query/legal \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the data retention policies?"}'

# Query Tech Docs
curl -X POST http://localhost:8080/api/multidoc/query/tech \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I authenticate with the API?"}'

# Query HR
curl -X POST http://localhost:8080/api/multidoc/query/hr \
  -H "Content-Type: application/json" \
  -d '{"question": "How many PTO days do new employees get?"}'
```

### `POST /api/multidoc/smart-query`
Auto-route to the best collection based on the question.

```bash
# Automatically routes to FAQ
curl -X POST http://localhost:8080/api/multidoc/smart-query \
  -H "Content-Type: application/json" \
  -d '{"question": "How much does the Professional plan cost?"}'

# Automatically routes to HR
curl -X POST http://localhost:8080/api/multidoc/smart-query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the parental leave policy?"}'
```

## Trade-offs

| Approach | Pros | Cons |
|----------|------|------|
| Single store | Simple setup | Context pollution, less precise |
| Multiple stores | Precise retrieval, isolation | More memory, routing complexity |
| Single store + metadata filter | Balance of both | Filter support varies by store |

## Source Files
- Service: [`MultiDocService.java`](../src/main/java/com/example/rag_spring_ai/multidoc/MultiDocService.java)
- Controller: [`MultiDocController.java`](../src/main/java/com/example/rag_spring_ai/multidoc/MultiDocController.java)

