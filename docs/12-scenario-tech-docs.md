# Real-World Scenario: Technical Documentation Assistant

## Business Context

Developer experience (DX) is a competitive advantage. Developers hate:
- Searching through 100-page API docs for one endpoint
- Not finding code examples in the right language
- Outdated or inconsistent documentation

A RAG-powered tech docs assistant turns static documentation into an interactive, conversational experience:

- **Natural language API search**: "How do I upload a file?"
- **Code generation**: Generate `curl` commands or SDK examples
- **Structured extraction**: Get endpoint specs in a structured format
- **Context-aware**: Understands your API's specific terminology

## Architecture

```
Developer Question
       ↓
┌──────────────────────────────────────────┐
│       Tech Docs Assistant                 │
│                                           │
│  API Documentation (chunked per endpoint) │
│           ↓                               │
│  Vector Store (medium chunks to keep      │
│  endpoint + parameters together)          │
│           ↓                               │
│  Developer-focused system prompt          │
│  • Code examples                          │
│  • curl commands                          │
│  • Structured endpoint specs              │
└──────────────────────────────────────────┘
```

## API Endpoints

### `POST /api/scenarios/techdocs/ask`
Ask a natural language question about the API.

```bash
# Authentication
curl -X POST http://localhost:8080/api/scenarios/techdocs/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I authenticate with the CloudFlow API?"}'

# Rate limits
curl -X POST http://localhost:8080/api/scenarios/techdocs/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the API rate limits for each plan?"}'

# Specific operation
curl -X POST http://localhost:8080/api/scenarios/techdocs/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I upload a document to a workspace?"}'

# Webhooks
curl -X POST http://localhost:8080/api/scenarios/techdocs/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I set up webhooks for document events?"}'
```

### `POST /api/scenarios/techdocs/endpoints`
Find endpoints for a feature — returns structured `ApiEndpoint` records.

```bash
# Document management endpoints
curl -X POST http://localhost:8080/api/scenarios/techdocs/endpoints \
  -H "Content-Type: application/json" \
  -d '{"feature": "document management"}'

# Workspace endpoints
curl -X POST http://localhost:8080/api/scenarios/techdocs/endpoints \
  -H "Content-Type: application/json" \
  -d '{"feature": "workspace operations"}'

# AI features
curl -X POST http://localhost:8080/api/scenarios/techdocs/endpoints \
  -H "Content-Type: application/json" \
  -d '{"feature": "AI summarization and question answering"}'
```

**Response:**
```json
[
  {
    "method": "POST",
    "path": "/workspaces/{workspace_id}/documents",
    "description": "Upload a document to a workspace",
    "parameters": "workspace_id (required), file (required), folder_path (optional), tags (optional)"
  },
  {
    "method": "GET",
    "path": "/workspaces/{workspace_id}/documents",
    "description": "List all documents in a workspace",
    "parameters": "workspace_id (required), type (optional), search (optional), page (optional)"
  }
]
```

### `POST /api/scenarios/techdocs/curl`
Generate a working `curl` command for a specific operation.

```bash
# Generate curl for authentication
curl -X POST http://localhost:8080/api/scenarios/techdocs/curl \
  -H "Content-Type: application/json" \
  -d '{"operation": "authenticate and get an access token"}'

# Generate curl for document upload
curl -X POST http://localhost:8080/api/scenarios/techdocs/curl \
  -H "Content-Type: application/json" \
  -d '{"operation": "upload a document to a workspace"}'

# Generate curl for AI summarization
curl -X POST http://localhost:8080/api/scenarios/techdocs/curl \
  -H "Content-Type: application/json" \
  -d '{"operation": "summarize documents using AI"}'
```

## Why This Is a Great RAG Use Case

1. **Structured source data** — API docs have consistent formatting (method, path, params)
2. **Clear retrieval units** — each endpoint is a natural "chunk"
3. **Factual answers** — less room for hallucination
4. **High developer value** — saves minutes of searching per query
5. **Easy to evaluate** — you can verify generated curl commands actually work

## Production Enhancements

- **Auto-sync**: Watch for documentation changes and re-ingest automatically
- **Multi-version**: Support docs for multiple API versions with metadata filtering
- **SDK generation**: Generate code in the developer's preferred language
- **Interactive playground**: Generate and execute API calls in a sandbox
- **Feedback loop**: Let developers flag incorrect answers

## Source Files
- Service: [`TechDocsService.java`](../src/main/java/com/example/rag_spring_ai/scenarios/techdocs/TechDocsService.java)
- Controller: [`TechDocsController.java`](../src/main/java/com/example/rag_spring_ai/scenarios/techdocs/TechDocsController.java)
- Document: [`api-guide.txt`](../src/main/resources/documents/techdocs/api-guide.txt)

