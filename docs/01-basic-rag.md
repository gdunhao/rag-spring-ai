# Demo 1: Basic RAG (Retrieval-Augmented Generation)

## What is RAG?

**RAG (Retrieval-Augmented Generation)** is a technique that enhances LLM responses by providing relevant external knowledge as context. Instead of relying solely on the model's training data (which can be outdated or incomplete), RAG retrieves relevant documents from a knowledge base and includes them in the prompt.

### Why RAG Matters

| Problem with plain LLMs | How RAG solves it |
|--------------------------|-------------------|
| Hallucination — makes up facts | Grounds answers in real documents |
| Outdated knowledge | Can use up-to-the-minute data |
| No access to private data | Connects to your own documents |
| No source attribution | Can cite where the answer came from |

## How This Demo Works

```
1. Document Loading
   spring-ai-overview.txt → TextReader → List<Document>

2. Chunking
   List<Document> → TokenTextSplitter → List<Document> (smaller chunks)

3. Embedding & Storage
   List<Document> → EmbeddingModel → float[] vectors → VectorStore

4. Query (when user asks a question)
   Question → EmbeddingModel → float[] vector
                                    ↓
                            VectorStore.similaritySearch()
                                    ↓
                            Relevant chunks retrieved
                                    ↓
                            Question + Context → LLM → Answer
```

## Spring AI Components Used

### `TextReader`
Reads a plain text file into a `Document` object. You can attach custom metadata.

```java
var reader = new TextReader(resource);
reader.getCustomMetadata().put("source", "spring-ai-overview.txt");
List<Document> documents = reader.get();
```

### `TokenTextSplitter`
Splits documents into smaller chunks based on token count. Default: 800 tokens per chunk with 350 token overlap.

```java
var splitter = new TokenTextSplitter();
List<Document> chunks = splitter.apply(documents);
```

### `QuestionAnswerAdvisor`
The magic of Spring AI — this advisor automatically:
1. Takes the user's question
2. Searches the VectorStore for relevant documents
3. Adds retrieved documents to the prompt as context
4. Passes everything to the LLM

```java
client.prompt()
    .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
    .user(question)
    .call()
    .content();
```

## API Endpoints

### `POST /api/basic/ask`
Ask a question using RAG.

```bash
curl -X POST http://localhost:8080/api/basic/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is RAG and how does it work?"}'
```

**Response:**
```json
{
  "question": "What is RAG and how does it work?",
  "answer": "RAG (Retrieval-Augmented Generation) works by: 1. Ingesting documents and splitting them into chunks, 2. Converting each chunk into a vector embedding, 3. Storing embeddings in a vector store, 4. When a query arrives, finding similar document chunks, 5. Including the retrieved chunks as context in the LLM prompt..."
}
```

### `POST /api/basic/ingest`
Manually trigger document ingestion.

```bash
curl -X POST http://localhost:8080/api/basic/ingest
```

## Try These Questions

```bash
# About Spring AI features
curl -X POST http://localhost:8080/api/basic/ask \
  -d '{"question": "What LLM providers does Spring AI support?"}'

# About vector stores
curl -X POST http://localhost:8080/api/basic/ask \
  -d '{"question": "What vector databases can I use with Spring AI?"}'

# About the RAG pattern benefits
curl -X POST http://localhost:8080/api/basic/ask \
  -d '{"question": "How does RAG reduce hallucination?"}'
```

## Source Files
- Service: [`BasicRagService.java`](../src/main/java/com/example/rag_spring_ai/basic/BasicRagService.java)
- Controller: [`BasicRagController.java`](../src/main/java/com/example/rag_spring_ai/basic/BasicRagController.java)
- Document: [`spring-ai-overview.txt`](../src/main/resources/documents/sample/spring-ai-overview.txt)

