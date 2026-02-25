# API Guard Architecture

API Guard is a cross-domain static analysis tool designed to bridge the gap between individual microservices by constructing a global dependency graph and enabling cross-service dataflow analysis with CodeQL.

## High-Level Overview

The system consists of three main components:
1.  **Java Analyzer (Scanner & Linker)**: Parses Spring Boot source code to identify REST endpoints and client calls.
2.  **C++ CLI (Orchestrator & Adapter)**: Manages the analysis lifecycle, invokes the Java Analyzer, and modifies CodeQL databases (TRAP files) to link call sites to definitions across service boundaries.
3.  **MCP Server (Exposer)**: A Model Context Protocol (MCP) server that exposes the generated dependency graph to LLMs, allowing them to query service relationships and details.

```mermaid
graph TD
    subgraph "Microservices Repo"
        ServiceA[Service A (Spring Boot)]
        ServiceB[Service B (Spring Boot)]
    end

    subgraph "Java Analyzer"
        Scanner[Source Parser]
        Linker[Dependency Graph Builder]
    end

    subgraph "C++ CLI"
        Orchestrator[Process Manager]
        Adapter[TRAP File Modifier]
    end

    subgraph "Analysis & Query"
        CodeQL[CodeQL Engine]
        MCPServer[MCP Server (Python)]
        LLM[LLM / AI Assistant]
    end

    ServiceA --> Scanner
    ServiceB --> Scanner
    Scanner --> Linker
    Linker -->|JSON Graph| Adapter
    Linker -->|JSON Graph| MCPServer
    Adapter -->|Enriched DBs| CodeQL
    MCPServer -->|Context| LLM
```

## Component Details

### 1. Java Analyzer (`dependency_analyzers/java/api_guard`)
*   **Technology**: Java 25, Maven, JavaFX (for optional GUI).
*   **Role**:
    *   **Scanning**: Uses `JavaParser` (via `com.github.javaparser`) to statically analyze Spring Boot controllers (`@RestController`, `@GetMapping`, etc.) and clients (`@FeignClient`, `RestTemplate`, `WebClient`).
    *   **Linking**: Matches client calls (e.g., `feignClient.getUsers()`) to producer endpoints (e.g., `GET /users`) based on URL paths and HTTP methods.
    *   **Output**: Produces a JSON representation of the microservice graph (`services`, `links`, `endpoints`).
*   **Key Classes**:
    *   `EndpointExtractor`: Extracts producer endpoints.
    *   `RestClientExtractor`: Extracts consumer calls.
    *   `AnalysisService`: Orchestrates the scanning and linking process.
    *   `Launcher`: Entry point for the application (CLI or GUI).

### 2. C++ CLI (`cli/api_guard`)
*   **Technology**: C++23, CMake, Boost (Process, Graph, DLL), `nlohmann/json`, `cxxopts`.
*   **Role**:
    *   **Orchestration**: Runs the Java Analyzer as a subprocess.
    *   **Database Management**: Interfaces with CodeQL CLI to create and manage databases.
    *   **TRAP Modification**: Parses CodeQL TRAP files (text-based relational data) and injects "synthetic" call edges that represent cross-service calls, effectively "fooling" CodeQL into seeing a monolithic application.
*   **Build System**: Uses `vcpkg` for dependency management and `CMake` for building the executable.

### 3. MCP Server (`server.py`)
*   **Technology**: Python 3.x, `mcp` (Model Context Protocol), `FastMCP`.
*   **Role**:
    *   Loads the JSON graph produced by the Java Analyzer.
    *   Exposes tools (`list_services`, `get_service_details`, `find_incoming_calls`) to AI assistants.
    *   Allows LLMs to "reason" about the architecture by querying the graph directly.
*   **Configuration**: Currently requires environment setup for paths (Java, JavaFX, JARs) - *Note: See `server.py` for specific hardcoded path requirements.*

## Data Flow

1.  **Input**: A directory containing multiple Spring Boot microservice projects.
2.  **Phase 1 (Scan)**: The Java Analyzer scans each project, extracting semantic models of endpoints and clients.
3.  **Phase 2 (Link)**: The Analyzer resolves internal dependencies (e.g., `Service A` calls `Service B`) and outputs a `graph.json`.
4.  **Phase 3 (Adapt)**: The C++ CLI uses the graph to rewrite CodeQL TRAP files, replacing "external" API calls with "internal" function calls to the target controller methods.
5.  **Phase 4 (Analyze)**: CodeQL runs taint tracking or dataflow queries on the modified databases, identifying vulnerabilities that span across services (e.g., SQL Injection starting in Service A and executing in Service B).

## Development Guidelines

*   **Java**: Ensure JDK 25 is installed. The project relies on preview features or recent language updates.
*   **C++**: Requires a modern compiler supporting C++23 (GCC 13+, Clang 17+, MSVC 2022).
*   **Dependencies**:
    *   JavaFX must be available (either via SDK or module path).
    *   `smartgraph` is a submodule dependency for the Java GUI.
