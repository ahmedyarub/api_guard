# API Guard System Architecture

This document describes the high-level architecture of the complete API Guard ecosystem, from the foundational code analysis engines up through the user-facing AI chat application.

## High-Level Architecture Diagram

```mermaid
flowchart TD
    %% User and UI
    User(("👤 User"))
    ReactApp["⚛️ React Client (Vite, port 5173)"]

    %% AI Components
    Ollama["🦙 Ollama (Local LLM: qwen3:0.6b, port 11434)"]
    Bridge["🌉 MCP HTTP Bridge (e.g., ollama-mcp-bridge, port 8000)"]
    MCPServer["🐍 MCP Server (server.py FastMCP, port 8001, SSE Transport)"]

    %% Core Analyzers
    JavaAnalyzer["☕ Java Analyzer (Spring Boot Scanner/Linker)"]
    CppCLI["⚙️ C++ CLI (Orchestrator/Adapter)"]
    CodeQL[("💾 CodeQL DB (Static Analysis)")]
    SourceCode["📁 Microservices (Source Code)"]

    %% Relationships
    User <-->|Chats / Selects Models| ReactApp

    %% Frontend Proxy flows
    ReactApp -->|"/api/tags (Fetch models)"| Ollama
    ReactApp -->|"/api/chat (Proxy)"| Bridge

    %% AI Bridge execution loop
    Bridge <-->|Generates prompts / Receives tool calls| Ollama
    Bridge <-->|Connects via SSE / Executes tools| MCPServer

    %% Backend Tooling
    MCPServer -->|Subprocess Execution| JavaAnalyzer
    JavaAnalyzer -->|Parses Code| SourceCode

    %% Other Core Components
    CppCLI -->|Manipulates/Queries| CodeQL
    CodeQL -->|Indexes| SourceCode
    JavaAnalyzer <.->|Collaborates for Links| CppCLI

    %% Styling
    classDef ui fill:#61dafb,stroke:#333,stroke-width:2px,color:#000
    classDef ai fill:#f9d371,stroke:#333,stroke-width:2px,color:#000
    classDef python fill:#4b8bbe,stroke:#333,stroke-width:2px,color:#fff
    classDef core fill:#e76f00,stroke:#333,stroke-width:2px,color:#fff
    classDef data fill:#9e9e9e,stroke:#333,stroke-width:2px,color:#fff

    class ReactApp ui
    class Ollama,Bridge ai
    class MCPServer python
    class JavaAnalyzer,CppCLI core
    class CodeQL,SourceCode data
```

## Components Breakdown

### 1. React Application (`react-ollama-client`)
The user-facing chat interface built with React and Vite. It provides a visual UI for the user to chat with the LLM, parse Markdown responses, and select the desired local model from a dropdown. It uses Vite's proxy to route requests to the backend architecture without encountering CORS issues.

### 2. Ollama (`qwen3:0.6b` / `llama3.2`)
The local Large Language Model (LLM) engine running on port `11434`. Ollama is responsible for understanding user intent, synthesizing answers, and, crucially, requesting function calls (tools) when it determines it needs to query the microservice architecture. The React app talks to Ollama directly (via the proxy) for model discovery (`/api/tags`).

### 3. MCP HTTP Bridge (`ollama-mcp-bridge`)
Since Ollama does not natively resolve or orchestrate Model Context Protocol (MCP) tools autonomously via its REST API, an intermediate bridge (running on port `8000`) is utilized. This bridge listens for OpenAI-compatible or Ollama-compatible `/api/chat` requests from the React frontend, proxies them to Ollama, and automatically intercepts and executes any `tool_calls` the LLM requests against the downstream MCP Server.

### 4. MCP Server (`server.py`)
A Python-based FastMCP server listening on port `8001` via Server-Sent Events (SSE). It defines the specific tools that the LLM is allowed to execute (e.g., `list_services`, `get_service_details`, `find_incoming_calls`). When the bridge invokes a tool, `server.py` executes the Java Analyzer as a subprocess, collects the JSON output, and returns it to the bridge.

### 5. API Guard Java Analyzer
A robust Java tool (`api_guard`) that acts as a Scanner and Linker. It parses Spring Boot projects and microservices source code, extracting configuration paths, Feign clients, HTTP methods, and service dependencies to construct a comprehensive JSON map of producers and consumers.

### 6. C++ CLI Adapter
A high-performance C++ command-line interface (`cli/api_guard/main.cpp`) responsible for orchestrating analysis workflows and adapting `.trap` files for CodeQL databases.

### 7. CodeQL Database
A relational database representation of the microservices source code. It is utilized for deep, semantic static analysis queries by the core architectural components.