# Demo 6: Structured Output

## The Problem with Free-Text Responses

By default, LLMs return unstructured text. This is fine for chatbots, but when building APIs or data pipelines, you need **structured data** — Java objects, not prose.

```
❌ Free text: "The pricing plans are Starter at $29/month, Professional at $79/month..."
✅ Structured: FaqEntry(question="...", answer="...", category="pricing")
```

## How Structured Output Works in Spring AI

Spring AI uses `BeanOutputConverter` under the hood:

1. **Schema generation**: Inspects the target Java record/class and generates a JSON schema
2. **Prompt augmentation**: Adds format instructions to the prompt telling the LLM to respond in JSON
3. **Response parsing**: Parses the LLM's JSON response into the target Java type

```
User Question
     ↓
+ "Respond in the following JSON format: {schema}"  ← added automatically
     ↓
LLM generates JSON response
     ↓
BeanOutputConverter.parse(json) → Java Record
```

## Using `.entity()` on ChatClient

### Single Object

```java
FaqEntry faq = client.prompt()
    .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .user("What pricing plans are available?")
    .call()
    .entity(FaqEntry.class);  // ← Magic happens here
```

### List of Objects

```java
List<LegalClause> clauses = client.prompt()
    .user("Find clauses about termination")
    .call()
    .entity(new ParameterizedTypeReference<List<LegalClause>>() {});
```

## Model Records Used

```java
public record FaqEntry(String question, String answer, String category) {}

public record LegalClause(String section, String title, String summary, String relevance) {}

public record ApiEndpoint(String method, String path, String description, String parameters) {}
```

**Design tips for output records:**
- Use `String` for fields the LLM fills (most flexible)
- Use enums sparingly (LLMs sometimes generate unexpected values)
- Keep records flat — avoid deep nesting
- Use descriptive field names — the LLM reads the schema

## API Endpoints

### `POST /api/structured/faq`
Extract a structured FAQ entry from a question.

```bash
curl -X POST http://localhost:8080/api/structured/faq \
  -H "Content-Type: application/json" \
  -d '{"question": "What pricing plans does CloudFlow offer?"}'
```

**Response:**
```json
{
  "question": "What pricing plans does CloudFlow offer?",
  "answer": "CloudFlow offers three plans: Starter ($29/month), Professional ($79/month), and Enterprise (custom pricing).",
  "category": "billing"
}
```

### `POST /api/structured/legal`
Extract structured legal clause information.

```bash
curl -X POST http://localhost:8080/api/structured/legal \
  -H "Content-Type: application/json" \
  -d '{"query": "data privacy and user content"}'
```

**Response:**
```json
[
  {
    "section": "4",
    "title": "User Content and Data",
    "summary": "Users retain ownership of uploaded content. CloudFlow only uses it to provide the service.",
    "relevance": "HIGH"
  }
]
```

### `POST /api/structured/api`
Extract structured API endpoint documentation.

```bash
curl -X POST http://localhost:8080/api/structured/api \
  -H "Content-Type: application/json" \
  -d '{"query": "document upload and management"}'
```

## When to Use Structured Output

| Use Case | Why Structured Output |
|----------|----------------------|
| API responses | Clients expect JSON schemas, not prose |
| Data extraction | Pull structured data from unstructured docs |
| Multi-step workflows | Pass LLM output to another service |
| Dashboards | Display data in tables, charts |
| Comparisons | Compare entities side by side |

## Source Files
- Service: [`StructuredOutputService.java`](../src/main/java/com/example/rag_spring_ai/structured/StructuredOutputService.java)
- Controller: [`StructuredOutputController.java`](../src/main/java/com/example/rag_spring_ai/structured/StructuredOutputController.java)
- Records: [`model/`](../src/main/java/com/example/rag_spring_ai/model/)

