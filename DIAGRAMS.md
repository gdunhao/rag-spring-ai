# Architecture Diagrams — RAG with Spring AI

> All diagrams use the **dark theme** for correct rendering on dark editor/viewer backgrounds (GitHub dark mode, IntelliJ, VS Code, Obsidian, etc.).

---

## Table of Contents

1. [System Architecture](#1-system-architecture)
2. [RAG Ingestion Pipeline](#2-rag-ingestion-pipeline)
3. [RAG Query Pipeline](#3-rag-query-pipeline)
4. [Chat Memory & Session Management](#4-chat-memory--session-management)
5. [Advisor Execution Chain](#5-advisor-execution-chain)
6. [Multi-Document RAG & Smart Routing](#6-multi-document-rag--smart-routing)
7. [Function / Tool Calling Flow](#7-function--tool-calling-flow)
8. [Metadata Filtering Flow](#8-metadata-filtering-flow)
9. [Structured Output Pipeline](#9-structured-output-pipeline)
10. [Module & Package Overview](#10-module--package-overview)

---

## 1. System Architecture

High-level view of the three-tier stack and how each component communicates.

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': {'fontSize': '14px'}}}%%
graph TB
    subgraph CLIENT["🖥️  Client"]
        HTTP["HTTP / curl\n:8080"]
    end

    subgraph APP["☕  Spring Boot 3.4  —  port 8080"]
        direction TB
        CTRL["REST Controllers\n(basic · ingest · vectorstore · chat\n advisor · structured · function\n multidoc · metadata · scenarios)"]
        SVC["Services"]
        CFG["Config\n(ChatClientConfig)"]
        ADV["Advisor Pipeline\n(QuestionAnswerAdvisor\n MessageChatMemoryAdvisor\n SafeGuardAdvisor)"]
        CTRL --> SVC
        SVC --> ADV
        CFG -.->|"builds"| SVC
    end

    subgraph OLLAMA["🤖  Ollama  —  port 11434"]
        direction LR
        CHAT["qwen3:4b\nChat / Reasoning"]
        EMBED["nomic-embed-text\nEmbeddings  (768-dim)"]
    end

    subgraph PG["🐘  PostgreSQL 16 + pgvector  —  port 5432"]
        VS[("vector_store\n(HNSW · COSINE)\n\nPersistent volume")]
    end

    HTTP --> CTRL
    ADV -->|"embed query"| EMBED
    ADV -->|"similarity search"| VS
    EMBED -->|"store vectors"| VS
    SVC -->|"chat / completion"| CHAT

    style CLIENT fill:#1a1a2e,stroke:#4a9eff,color:#e0e0e0
    style APP fill:#16213e,stroke:#4a9eff,color:#e0e0e0
    style OLLAMA fill:#0f3460,stroke:#4a9eff,color:#e0e0e0
    style PG fill:#1a1a2e,stroke:#4a9eff,color:#e0e0e0
```

---

## 2. RAG Ingestion Pipeline

How raw documents are loaded, split, embedded, and stored in the vector database.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    subgraph INPUT["📄  Input Sources"]
        TXT["Text files\n(.txt)"]
        JSON["JSON files\n(.json)"]
        PDF["PDF / DOCX\n(Tika / PDFBox)"]
    end

    subgraph LOAD["📖  Document Readers"]
        TR["TextReader"]
        JR["JsonReader"]
        TikaR["TikaDocumentReader\nPdfDocumentReader"]
    end

    subgraph SPLIT["✂️  Token Text Splitter"]
        SP["TokenTextSplitter\n800 tokens / chunk\n350 token overlap"]
    end

    subgraph META["🏷️  Metadata Enrichment"]
        MD["Add source, collection,\nproduct, version, category\nmetadata fields"]
    end

    subgraph EMBED["🔢  Embedding"]
        EM["EmbeddingModel\n(nomic-embed-text via Ollama)\n→ 768-dim float vector"]
    end

    subgraph STORE["🗄️  Vector Store"]
        PG[("PgVectorStore\nPostgreSQL + pgvector\nHNSW index\nCOSINE distance")]
    end

    TXT --> TR
    JSON --> JR
    PDF --> TikaR
    TR & JR & TikaR --> SP
    SP --> MD
    MD --> EM
    EM -->|"vectorStore.add(chunks)"| PG

    style INPUT fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
    style LOAD fill:#1a2a4a,stroke:#4a9eff,color:#e0e0e0
    style SPLIT fill:#1a2a4a,stroke:#4a9eff,color:#e0e0e0
    style META fill:#1a2a4a,stroke:#4a9eff,color:#e0e0e0
    style EMBED fill:#1e3a5f,stroke:#e67e22,color:#e0e0e0
    style STORE fill:#1e3a5f,stroke:#2ecc71,color:#e0e0e0
```

---

## 3. RAG Query Pipeline

Sequence of operations from a user's question to a grounded answer.

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    actor User
    participant Ctrl as REST Controller
    participant Svc as Service
    participant QAAdv as QuestionAnswerAdvisor
    participant EMod as EmbeddingModel<br/>(nomic-embed-text)
    participant PGVec as PgVectorStore
    participant LLM as ChatModel<br/>(qwen3:4b)

    User->>Ctrl: POST /api/basic/ask<br/>{"question": "What is RAG?"}
    Ctrl->>Svc: ask(question)
    Svc->>QAAdv: advisors(QuestionAnswerAdvisor)

    Note over QAAdv,EMod: Retrieval phase
    QAAdv->>EMod: embed(question)
    EMod-->>QAAdv: float[768] queryVector

    QAAdv->>PGVec: similaritySearch(queryVector, topK=4)
    PGVec-->>QAAdv: List<Document> relevantChunks

    Note over QAAdv,LLM: Augmentation + Generation phase
    QAAdv->>LLM: systemPrompt + relevantChunks + question
    LLM-->>QAAdv: grounded answer (text)

    QAAdv-->>Svc: answer
    Svc-->>Ctrl: answer
    Ctrl-->>User: 200 OK  {"answer": "..."}
```

---

## 4. Chat Memory & Session Management

How `MessageChatMemoryAdvisor` maintains per-session conversation history with an in-memory repository.

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    actor UserA as User (session-1)
    actor UserB as User (session-2)
    participant Ctrl as ChatMemoryController
    participant Svc as ChatMemoryService
    participant MemAdv as MessageChatMemoryAdvisor
    participant MemRepo as InMemoryChatMemoryRepository
    participant QAAdv as QuestionAnswerAdvisor
    participant LLM as qwen3:4b

    Note over MemRepo: One shared repository<br/>for all sessions

    UserA->>Ctrl: POST /api/chat/session-1<br/>"What is Spring AI?"
    Ctrl->>Svc: chat("session-1", message)
    Svc->>MemAdv: param(CONVERSATION_ID="session-1")
    MemAdv->>MemRepo: load history for session-1  → []
    MemAdv->>QAAdv: retrieve context
    QAAdv-->>MemAdv: relevant docs
    MemAdv->>LLM: [] + context + "What is Spring AI?"
    LLM-->>MemAdv: "Spring AI is..."
    MemAdv->>MemRepo: save [Q: "What is Spring AI?" / A: "Spring AI is..."]
    MemAdv-->>Ctrl: "Spring AI is..."

    UserB->>Ctrl: POST /api/chat/session-2<br/>"Hello, I'm Alice"
    Note over MemAdv,MemRepo: Independent session — no cross-contamination
    MemAdv->>MemRepo: load history for session-2  → []
    MemAdv->>LLM: [] + "Hello, I'm Alice"
    LLM-->>MemAdv: "Hello Alice!"
    MemAdv->>MemRepo: save [Q: "Hello, I'm Alice" / A: "Hello Alice!"]

    UserA->>Ctrl: POST /api/chat/session-1<br/>"What was my previous question?"
    MemAdv->>MemRepo: load history for session-1
    MemRepo-->>MemAdv: [Q: "What is Spring AI?" / A: "Spring AI is..."]
    MemAdv->>LLM: [prior exchange] + "What was my previous question?"
    LLM-->>MemAdv: "Your previous question was about Spring AI"
    MemAdv-->>Ctrl: "Your previous question was about Spring AI"
```

---

## 5. Advisor Execution Chain

Advisors implement a chain-of-responsibility pattern, executing in declaration order before and after the LLM call.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart TD
    REQ["📨  Incoming User Request"]

    subgraph CHAIN["Advisor Chain  (ordered)"]
        direction TB
        SG["1️⃣  SafeGuardAdvisor\n──────────────────\nChecks banned words:\nhack · exploit · injection\nbypass security\n\n❌ Blocked → return error\n✅ Pass → continue"]
        QA["2️⃣  QuestionAnswerAdvisor\n──────────────────\nEmbeds user query\nSearches vector store (topK, threshold)\nInjects retrieved docs into prompt context"]
        MEM["3️⃣  MessageChatMemoryAdvisor\n──────────────────\nLoads prior conversation history\nAppends to prompt messages\nSaves new exchange after response"]
    end

    LLM["🤖  LLM Call\n(qwen3:4b)\nsystem + history\n+ context + question"]

    RESP["📤  Response\nPost-processing\n(memory save, logging)"]

    REQ --> SG
    SG -->|"✅ allowed"| QA
    SG -->|"❌ blocked"| BLOCKED["⛔ Blocked\nResponse"]
    QA --> MEM
    MEM --> LLM
    LLM --> RESP

    style REQ fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
    style CHAIN fill:#0d1b2a,stroke:#555,color:#e0e0e0
    style SG fill:#3d1a1a,stroke:#e74c3c,color:#e0e0e0
    style QA fill:#1a3d1a,stroke:#2ecc71,color:#e0e0e0
    style MEM fill:#1a2a4a,stroke:#4a9eff,color:#e0e0e0
    style LLM fill:#2d1a4a,stroke:#9b59b6,color:#e0e0e0
    style RESP fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
    style BLOCKED fill:#3d1a1a,stroke:#e74c3c,color:#e0e0e0
```

---

## 6. Multi-Document RAG & Smart Routing

Four isolated in-memory `SimpleVectorStore` collections with keyword-based automatic routing.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart TB
    Q["❓ User Question"]

    subgraph ROUTER["🧭  Smart Router (keyword heuristics)"]
        direction LR
        K1["price · plan · billing\nfeature · support"]
        K2["terms · legal · liability\npolicy · compliance · refund"]
        K3["api · endpoint · webhook\nauth · rest"]
        K4["pto · leave · salary\nbenefits · hr · employee"]
    end

    subgraph COLS["📚  Document Collections  (SimpleVectorStore per collection)"]
        direction LR
        FAQ["📋 FAQ Collection\ncustomer-faq.txt"]
        LEGAL["⚖️  Legal Collection\nterms-of-service.txt"]
        TECH["🔧  Tech Docs Collection\napi-guide.txt"]
        HR["👥  HR Collection\nhr-policies.txt"]
    end

    LLM["🤖  qwen3:4b\n(system prompt scoped\nto detected collection)"]
    ANS["✅  Answer + detectedCollection"]

    Q --> ROUTER
    K1 -->|"→ faq"| FAQ
    K2 -->|"→ legal"| LEGAL
    K3 -->|"→ tech"| TECH
    K4 -->|"→ hr"| HR
    FAQ & LEGAL & TECH & HR -->|"retrieve top-K chunks"| LLM
    LLM --> ANS

    style Q fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
    style ROUTER fill:#0d1b2a,stroke:#e67e22,color:#e0e0e0
    style COLS fill:#0d1b2a,stroke:#2ecc71,color:#e0e0e0
    style FAQ fill:#1a3d1a,stroke:#2ecc71,color:#e0e0e0
    style LEGAL fill:#3d2a1a,stroke:#e67e22,color:#e0e0e0
    style TECH fill:#1a2a3d,stroke:#4a9eff,color:#e0e0e0
    style HR fill:#2a1a3d,stroke:#9b59b6,color:#e0e0e0
    style LLM fill:#2d1a4a,stroke:#9b59b6,color:#e0e0e0
    style ANS fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
```

---

## 7. Function / Tool Calling Flow

Spring AI inspects `@Tool`-annotated methods, generates a JSON schema, and manages multi-round LLM ↔ tool interactions automatically.

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    actor User
    participant Svc as FunctionCallingService
    participant LLM as qwen3:4b
    participant Spring as Spring AI<br/>Tool Engine
    participant Tool as @Tool Methods<br/>(SupportTools / WeatherTools)
    participant QAAdv as QuestionAnswerAdvisor

    User->>Svc: POST /api/function/support<br/>"My account is locked"

    Note over Svc,QAAdv: RAG retrieval first
    Svc->>QAAdv: retrieve relevant FAQ docs
    QAAdv-->>Svc: context chunks

    Note over Svc,LLM: Round 1 — LLM decides to call a tool
    Svc->>LLM: system + context + user message<br/>+ tool schemas (createTicket, lookupOrder)
    LLM-->>Spring: tool_call: createTicket<br/>{"customerName":"..","issue":"..","priority":"HIGH"}

    Note over Spring,Tool: Tool execution
    Spring->>Tool: createTicket(TicketRequest)
    Tool-->>Spring: TicketResponse {ticketId: "TKT-A1B2C3D4",<br/>status: "OPEN", createdAt: "..."}

    Note over Spring,LLM: Round 2 — LLM uses tool result
    Spring->>LLM: original messages + tool result
    LLM-->>Svc: "I've created ticket TKT-A1B2C3D4 for your issue..."

    Svc-->>User: Final grounded + tool-enhanced response
```

---

## 8. Metadata Filtering Flow

Documents are tagged with structured metadata at ingestion time; `FilterExpressionBuilder` restricts vector search to matching documents only.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    subgraph INGESTION["📥  Ingestion (PostConstruct)"]
        direction TB
        D1["CloudFlow v2.0 release notes\nproduct=cloudflow · version=2.0\ncategory=release-notes · year=2025"]
        D2["CloudFlow v2.5 release notes\nproduct=cloudflow · version=2.5\ncategory=release-notes · year=2026"]
        D3["CloudFlow API rate limits\nproduct=cloudflow · category=api-docs"]
        D4["DataSync Pro v1.0\nproduct=datasync · version=1.0"]
        D5["DataSync Pro v1.5\nproduct=datasync · version=1.5"]
        D6["CloudFlow security guide\nproduct=cloudflow · category=security"]
    end

    PGV[("PgVectorStore\n(all 6 docs with metadata)")]

    subgraph QUERY["🔍  Filtered Query"]
        direction TB
        FEB["FilterExpressionBuilder\n.eq('product', 'cloudflow')"]
        SR["SearchRequest\n  query: user question\n  topK: 5\n  filterExpression: product=cloudflow"]
        RESULTS["Only cloudflow docs\nreturned — datasync docs\ncompletely excluded"]
    end

    LLM["🤖  qwen3:4b\n(scoped to cloudflow context)"]
    ANS["✅  Product-scoped answer"]

    D1 & D2 & D3 & D4 & D5 & D6 --> PGV
    PGV --> QUERY
    FEB --> SR
    SR --> RESULTS
    RESULTS --> LLM
    LLM --> ANS

    style INGESTION fill:#0d1b2a,stroke:#4a9eff,color:#e0e0e0
    style QUERY fill:#0d1b2a,stroke:#e67e22,color:#e0e0e0
    style PGV fill:#1e3a5f,stroke:#2ecc71,color:#e0e0e0
    style FEB fill:#3d2a1a,stroke:#e67e22,color:#e0e0e0
    style SR fill:#3d2a1a,stroke:#e67e22,color:#e0e0e0
    style RESULTS fill:#1a3d1a,stroke:#2ecc71,color:#e0e0e0
    style LLM fill:#2d1a4a,stroke:#9b59b6,color:#e0e0e0
    style ANS fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
```

---

## 9. Structured Output Pipeline

`ChatClient.call().entity(Type.class)` uses `BeanOutputConverter` to inject format instructions into the prompt and parse the JSON response into a typed Java record.

```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    Q["❓  User Query\ne.g. 'data privacy terms'"]

    subgraph SPRING["Spring AI  BeanOutputConverter"]
        direction TB
        FI["1. Generate format instructions\nfrom target Java type schema\n(JSON Schema → prompt suffix)"]
        PROMPT["2. Build prompt\nsystem + context + query\n+ format instructions"]
    end

    QAAdv["QuestionAnswerAdvisor\n(retrieves relevant docs)"]
    LLM["🤖  qwen3:4b\nResponds with\nstructured JSON"]

    subgraph PARSE["Deserialization"]
        direction TB
        J["JSON response\n{\"section\":\"§7\",\"title\":\"Privacy\"\n\"summary\":\"...\",\"relevance\":\"HIGH\"}"]
        REC["Java Record\nLegalClause(\n  section, title,\n  summary, relevance\n)"]
        J --> REC
    end

    ANS["✅  List&lt;LegalClause&gt;\nor FaqEntry\nor List&lt;ApiEndpoint&gt;"]

    Q --> QAAdv --> SPRING
    FI --> PROMPT
    PROMPT --> LLM
    LLM --> PARSE
    REC --> ANS

    style Q fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
    style SPRING fill:#0d1b2a,stroke:#e67e22,color:#e0e0e0
    style QAAdv fill:#1a3d1a,stroke:#2ecc71,color:#e0e0e0
    style LLM fill:#2d1a4a,stroke:#9b59b6,color:#e0e0e0
    style PARSE fill:#1a2a3d,stroke:#4a9eff,color:#e0e0e0
    style ANS fill:#1e3a5f,stroke:#4a9eff,color:#e0e0e0
```

---

## 10. Module & Package Overview

Full package map showing each module's controller/service pair and the shared infrastructure they depend on.

```mermaid
%%{init: {'theme': 'dark'}}%%
graph TB
    subgraph CORE["⚙️  Core Infrastructure"]
        direction LR
        CC["ChatClientConfig\n(default ChatClient bean)"]
        PGV["PgVectorStore\n(auto-configured\nby Spring AI starter)"]
        EM["EmbeddingModel\n(nomic-embed-text)"]
        CM["ChatModel\n(qwen3:4b)"]
        CC --> CM
        PGV --> EM
    end

    subgraph MODELS["📐  Shared Model Records"]
        direction LR
        RR["RagResponse"]
        FE["FaqEntry"]
        LC["LegalClause"]
        AE["ApiEndpoint"]
        PI["PolicyInfo"]
        MR["MessageRequest\nQuestionRequest\nQueryRequest"]
    end

    subgraph DEMOS["🧪  Capability Demos"]
        direction TB
        D1["basic\nBasicRagController\nBasicRagService"]
        D2["ingestion\nIngestionController\nIngestionService"]
        D3["vectorstore\nVectorStoreController\nVectorStoreService"]
        D4["memory\nChatMemoryController\nChatMemoryService"]
        D5["advisor\nAdvisorController\nAdvisorService"]
        D6["structured\nStructuredOutputController\nStructuredOutputService"]
        D7["function\nFunctionCallingController\nFunctionCallingService\nFunctionConfig"]
        D8["multidoc\nMultiDocController\nMultiDocService"]
        D9["metadata\nMetadataFilterController\nMetadataFilterService"]
    end

    subgraph SCENARIOS["🌍  Real-World Scenarios"]
        direction TB
        S1["scenarios/support\nSupportController\nSupportService"]
        S2["scenarios/legal\nLegalSearchController\nLegalSearchService"]
        S3["scenarios/techdocs\nTechDocsController\nTechDocsService"]
        S4["scenarios/hr\nHrPolicyController\nHrPolicyService"]
    end

    CORE --> DEMOS
    CORE --> SCENARIOS
    MODELS --> D6 & D7 & S2 & S3 & S4

    style CORE fill:#1a2a4a,stroke:#4a9eff,color:#e0e0e0
    style MODELS fill:#1a2a3d,stroke:#9b59b6,color:#e0e0e0
    style DEMOS fill:#0d1b2a,stroke:#2ecc71,color:#e0e0e0
    style SCENARIOS fill:#1a2a1a,stroke:#e67e22,color:#e0e0e0
    style D1 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D2 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D3 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D4 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D5 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D6 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D7 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D8 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style D9 fill:#163216,stroke:#2ecc71,color:#e0e0e0
    style S1 fill:#2a1a16,stroke:#e67e22,color:#e0e0e0
    style S2 fill:#2a1a16,stroke:#e67e22,color:#e0e0e0
    style S3 fill:#2a1a16,stroke:#e67e22,color:#e0e0e0
    style S4 fill:#2a1a16,stroke:#e67e22,color:#e0e0e0
```

---

## Docker Compose Service Dependencies

Startup order and health-check dependencies between Docker services.

```mermaid
%%{init: {'theme': 'dark'}}%%
graph LR
    PG["🐘  rag-postgres\npgvector/pgvector:pg16\nport 5432\n\nhealthcheck: pg_isready"]
    OLL["🤖  rag-ollama\nollama/ollama:latest\nport 11434\n\nhealthcheck: ollama list"]
    INIT["⚙️  rag-ollama-init\n(one-shot)\n\npulls qwen3:4b\npulls nomic-embed-text\nthen exits"]
    APP["☕  Spring Boot App\n(run separately)\nport 8080"]

    OLL -->|"depends_on\ncondition: healthy"| INIT
    PG & OLL -->|"must be running"| APP

    style PG fill:#1e3a5f,stroke:#2ecc71,color:#e0e0e0
    style OLL fill:#2d1a4a,stroke:#9b59b6,color:#e0e0e0
    style INIT fill:#3d2a1a,stroke:#e67e22,color:#e0e0e0
    style APP fill:#1a3d1a,stroke:#4a9eff,color:#e0e0e0
```

