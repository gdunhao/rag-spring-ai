# Demo 3: Vector Store Operations

## What is a Vector Store?

A **vector store** (or vector database) is a specialized storage system designed to efficiently store, index, and query high-dimensional vectors. In the context of RAG:

- **Documents** are converted to **vectors** (arrays of floating-point numbers) by an embedding model
- **Vectors** that are semantically similar are close together in the vector space
- **Queries** are also converted to vectors, and the store finds the most similar document vectors

### Semantic Search vs. Keyword Search

| Keyword Search | Semantic/Vector Search |
|----------------|----------------------|
| Matches exact words | Matches meaning |
| "car" won't find "automobile" | "car" finds "automobile", "vehicle" |
| Fast, but brittle | Slightly slower, but much smarter |
| SQL LIKE / full-text | Vector cosine similarity |

## SimpleVectorStore

This demo uses `SimpleVectorStore` — Spring AI's in-memory vector store. It's perfect for development and demos but not suitable for production (no persistence, no scaling).

```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return new SimpleVectorStore(embeddingModel);
}
```

### Production Vector Stores

| Store | Type | Best For |
|-------|------|----------|
| **PgVector** | PostgreSQL extension | Already using PostgreSQL |
| **Chroma** | Standalone | Python ecosystem, easy setup |
| **Milvus** | Standalone | High-scale, production workloads |
| **Pinecone** | Cloud SaaS | Fully managed, zero ops |
| **Weaviate** | Standalone | GraphQL API, hybrid search |
| **Redis** | In-memory + persistence | Low-latency, existing Redis infra |
| **Qdrant** | Standalone | Rust-based, high performance |

## Key Operations

### 1. Adding Documents

```java
vectorStore.add(List.of(
    new Document("Java is a programming language", Map.of("topic", "java")),
    new Document("Python is great for data science", Map.of("topic", "python"))
));
```

### 2. Similarity Search

```java
// Find the 3 most similar documents to the query
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query("What programming language should I learn?")
        .withTopK(3)
);
```

### 3. Similarity Threshold

```java
// Only return documents with similarity score >= 0.7
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query("container orchestration")
        .withTopK(10)
        .withSimilarityThreshold(0.7)
);
```

### 4. Understanding Embeddings

```java
float[] embedding = embeddingModel.embed("Hello world");
// embedding.length → 768 (for nomic-embed-text)
// Each dimension captures a semantic feature
```

## API Endpoints

### `POST /api/vectorstore/add-samples`
Add 6 sample documents about programming topics.

```bash
curl -X POST http://localhost:8080/api/vectorstore/add-samples
```

### `GET /api/vectorstore/search`
Perform similarity search.

```bash
# Search for programming languages
curl "http://localhost:8080/api/vectorstore/search?query=programming+languages&topK=3"

# Search for DevOps tools
curl "http://localhost:8080/api/vectorstore/search?query=container+deployment&topK=2"
```

### `GET /api/vectorstore/search-threshold`
Search with a minimum similarity threshold.

```bash
# High threshold — only very similar results
curl "http://localhost:8080/api/vectorstore/search-threshold?query=machine+learning&threshold=0.8"

# Lower threshold — more results, less precision
curl "http://localhost:8080/api/vectorstore/search-threshold?query=machine+learning&threshold=0.5"
```

### `GET /api/vectorstore/embedding-info`
Inspect embedding dimensions and sample values.

```bash
curl "http://localhost:8080/api/vectorstore/embedding-info?text=Hello+world"
```

**Response:**
```json
{
  "text": "Hello world",
  "dimensions": 768,
  "sampleValues": [0.023, -0.041, 0.078],
  "model": "nomic-embed-text"
}
```

## Experiments to Try

1. **Semantic matching**: Search for "web framework" and see that Spring Boot is returned (even though "web framework" isn't in its description)
2. **Threshold tuning**: Try different thresholds to see how it affects result quality
3. **Embedding inspection**: Compare embedding dimensions for different models

## Source Files
- Service: [`VectorStoreService.java`](../src/main/java/com/example/rag_spring_ai/vectorstore/VectorStoreService.java)
- Controller: [`VectorStoreController.java`](../src/main/java/com/example/rag_spring_ai/vectorstore/VectorStoreController.java)

