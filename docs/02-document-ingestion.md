# Demo 2: Document Ingestion

## Overview

Before RAG can answer questions, documents must be **ingested** — read, split into chunks, embedded, and stored. This demo shows different ingestion strategies for various document formats.

## The Ingestion Pipeline

```
Source File → Reader → List<Document> → Splitter → List<Document> → VectorStore
                                (raw)              (chunks)         (embedded)
```

Each step is configurable and can be swapped independently.

## Document Readers

### 1. `TextReader` — Plain Text Files

The simplest reader. Reads the entire file as a single `Document`.

```java
var reader = new TextReader(resource);
reader.getCustomMetadata().put("source", "filename.txt");
List<Document> docs = reader.get();  // Usually 1 document
```

**Best for:** README files, plain text exports, simple knowledge bases.

### 2. `JsonReader` — JSON Files

Reads JSON arrays where each object becomes a `Document`. You specify which fields to use.

```java
// JSON: [{"title": "...", "content": "...", "category": "..."}]
var reader = new JsonReader(resource, "title", "content", "category");
List<Document> docs = reader.get();  // One document per JSON object
```

**Best for:** Structured data exports, FAQ databases, product catalogs.

### 3. `PagePdfDocumentReader` — PDF Files

Reads PDFs with one `Document` per page. Requires the `spring-ai-pdf-document-reader` dependency.

```java
var reader = new PagePdfDocumentReader(resource);
List<Document> docs = reader.get();  // One document per PDF page
```

**Best for:** Reports, research papers, manuals with page-level structure.

### 4. `TikaDocumentReader` — Multi-Format (PDF, DOCX, HTML, etc.)

Apache Tika can read 1000+ file formats. Requires the `spring-ai-tika-document-reader` dependency.

```java
var reader = new TikaDocumentReader(resource);
List<Document> docs = reader.get();
```

**Best for:** Mixed-format document collections, when you don't know the format in advance.

## Chunking Strategies

### Why Chunk?

- Embedding models have **token limits** (typically 512-8192 tokens)
- Smaller chunks produce **more focused embeddings**
- Better retrieval precision — you get the exact relevant paragraph, not the entire document
- Overlap between chunks prevents information loss at boundaries

### `TokenTextSplitter` Parameters

```java
TokenTextSplitter.builder()
    .withChunkSize(800)            // target tokens per chunk
    .withMinChunkSizeChars(350)    // minimum chunk size in characters
    .withMinChunkLengthToEmbed(5)  // skip very short chunks
    .withMaxNumChunks(100)         // maximum chunks per document
    .withKeepSeparator(true)       // preserve paragraph separators
    .build();
```

### Chunking Guidelines

| Document Type | Recommended Chunk Size | Overlap | Rationale |
|---------------|----------------------|---------|-----------|
| FAQ / Q&A | 200-400 tokens | Minimal | Each Q&A pair is self-contained |
| Legal docs | 300-500 tokens | High | Clauses reference each other |
| Technical docs | 400-800 tokens | Medium | Code examples need context |
| Prose / articles | 500-1000 tokens | Medium | Paragraphs are natural units |

## API Endpoints

### `POST /api/ingest/text`
Ingest a plain text file with default chunking.

```bash
curl -X POST http://localhost:8080/api/ingest/text
```

**Response:**
```json
{
  "source": "spring-ai-overview.txt",
  "format": "text",
  "documentsRead": 1,
  "chunksCreated": 3,
  "status": "ingested"
}
```

### `POST /api/ingest/json`
Ingest a JSON file (each object becomes a document).

```bash
curl -X POST http://localhost:8080/api/ingest/json
```

### `POST /api/ingest/custom-chunking`
Ingest with custom chunk sizes.

```bash
# Small chunks (200 tokens)
curl -X POST "http://localhost:8080/api/ingest/custom-chunking?chunkSize=200&minChunkSize=30"

# Large chunks (1000 tokens)
curl -X POST "http://localhost:8080/api/ingest/custom-chunking?chunkSize=1000&minChunkSize=100"
```

## Key Takeaway

The quality of your RAG system depends heavily on ingestion. Experiment with:
- Different chunk sizes for your specific documents
- Adding rich metadata during ingestion
- Pre-processing documents (cleaning, formatting) before ingestion

## Source Files
- Service: [`IngestionService.java`](../src/main/java/com/example/rag_spring_ai/ingestion/IngestionService.java)
- Controller: [`IngestionController.java`](../src/main/java/com/example/rag_spring_ai/ingestion/IngestionController.java)

