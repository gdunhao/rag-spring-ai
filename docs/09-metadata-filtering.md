# Demo 9: Metadata Filtering

## What is Metadata Filtering?

Every document in a vector store can carry **metadata** — key-value pairs that describe the document:

```java
new Document(
    "CloudFlow v2.5 added AI features...",
    Map.of(
        "product", "cloudflow",
        "version", "2.5",
        "category", "release-notes",
        "year", "2026"
    )
);
```

When performing similarity search, you can **filter results** based on metadata values *before* the similarity ranking. This is like adding a `WHERE` clause to your vector search.

## Why Metadata Filtering Matters

| Use Case | Metadata Filter |
|----------|----------------|
| Multi-tenant SaaS | `tenantId == "acme-corp"` |
| Document versioning | `version == "2.5"` |
| Category filtering | `category == "security"` |
| Date range queries | `year >= 2025` |
| Access control | `accessLevel IN ["public", "internal"]` |

Without metadata filtering, the vector store returns the most semantically similar documents **regardless of context**. With filtering, you get relevant documents *within the right scope*.

## FilterExpressionBuilder

Spring AI provides a type-safe builder for filter expressions:

```java
var b = new FilterExpressionBuilder();

// Simple equality
b.eq("product", "cloudflow").build()

// AND combination
b.and(b.eq("product", "cloudflow"), b.eq("category", "security")).build()

// IN operator
b.in("category", "release-notes", "security").build()

// Comparison operators
b.gte("year", 2025).build()  // year >= 2025
```

## Using Filters with SearchRequest

```java
var filterBuilder = new FilterExpressionBuilder();

List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query("What new features were added?")
        .withTopK(5)
        .withFilterExpression(
            filterBuilder.eq("product", "cloudflow").build()
        )
);
```

## Using Filters with RAG

```java
SearchRequest searchRequest = SearchRequest.query(question)
    .withTopK(3)
    .withFilterExpression(
        filterBuilder.eq("product", "cloudflow").build()
    );

client.prompt()
    .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest))
    .user(question)
    .call()
    .content();
```

## Documents in This Demo

| Content | Product | Version | Category |
|---------|---------|---------|----------|
| CloudFlow v2.0 features | cloudflow | 2.0 | release-notes |
| CloudFlow v2.5 features | cloudflow | 2.5 | release-notes |
| CloudFlow API rate limits | cloudflow | 2.5 | api-docs |
| DataSync Pro v1.0 features | datasync | 1.0 | release-notes |
| DataSync Pro v1.5 features | datasync | 1.5 | release-notes |
| CloudFlow security best practices | cloudflow | 2.5 | security |

## API Endpoints

### `GET /api/metadata/search/product`
Search filtered by product name.

```bash
# Only CloudFlow documents
curl "http://localhost:8080/api/metadata/search/product?query=new+features&product=cloudflow"

# Only DataSync documents
curl "http://localhost:8080/api/metadata/search/product?query=new+features&product=datasync"
```

### `GET /api/metadata/search/category`
Search filtered by document category.

```bash
# Only release notes
curl "http://localhost:8080/api/metadata/search/category?query=AI+features&category=release-notes"

# Only security documents
curl "http://localhost:8080/api/metadata/search/category?query=best+practices&category=security"
```

### `POST /api/metadata/ask`
RAG query filtered by product.

```bash
# Ask about CloudFlow only
curl -X POST http://localhost:8080/api/metadata/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What new features were added in the latest version?", "product": "cloudflow"}'

# Ask about DataSync only
curl -X POST http://localhost:8080/api/metadata/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What databases are supported?", "product": "datasync"}'
```

## Vector Store Filter Support

| Vector Store | Filter Support |
|-------------|---------------|
| SimpleVectorStore | Basic (eq, in) |
| PgVector | Full SQL-like expressions |
| Chroma | Full metadata filtering |
| Pinecone | Full metadata filtering |
| Milvus | Full with boolean expressions |
| Weaviate | Full with GraphQL filters |

## Source Files
- Service: [`MetadataFilterService.java`](../src/main/java/com/example/rag_spring_ai/metadata/MetadataFilterService.java)
- Controller: [`MetadataFilterController.java`](../src/main/java/com/example/rag_spring_ai/metadata/MetadataFilterController.java)

