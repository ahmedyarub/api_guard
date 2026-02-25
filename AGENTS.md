# AI Agent Instructions

This file contains instructions for AI agents working on the API Guard repository.

## Role
You are an expert software engineer specializing in static analysis, distributed systems, and compiler technology (Java/C++). Your goal is to maintain and enhance the API Guard tool, ensuring correctness, performance, and code quality.

## Codebase Map

*   **`cli/api_guard/`**: The C++ CLI orchestrator. Handles process management and CodeQL database adaptation.
*   **`dependency_analyzers/java/api_guard/`**: The Java analyzer. Parses Spring Boot code to extract dependency graphs.
*   **`server.py`**: The Model Context Protocol (MCP) server. Exposes the analysis results to AI assistants.
*   **`samples/`**: Contains sample microservice applications used for integration testing.

## Workflow

1.  **Understand the Architecture**: Read `ARCHITECTURE.md` to grasp the component interactions.
2.  **Verify Before Changing**: Always inspect the current state of files (`read_file`, `list_files`) before proposing edits.
3.  **Test Your Changes**:
    *   For Java changes: Run `mvn test`.
    *   For C++ changes: Build with `cmake` and run the executable.
    *   For Python/Integration: Run `pytest` (ensure submodules are initialized).
4.  **Respect Configuration**:
    *   Do not modify build artifacts (e.g., `target/`, `build/`) directly.
    *   Be cautious with hardcoded paths in scripts (e.g., `server.py`); prefer configuration files or environment variables where possible.

## Style Guide

*   **Java**: Follow standard Java naming conventions (CamelCase for classes, camelCase for methods/vars). Use Java 25 features where appropriate.
*   **C++**: Use Modern C++ (C++20/23). Prefer `std::filesystem`, `std::format`, and RAII.
*   **Python**: Follow PEP 8. Use type hints (`typing`) for all function signatures.

## Testing Requirements

*   **Unit Tests**: New logic should have accompanying unit tests.
*   **Integration Tests**: Major changes to the analysis logic must be verified against the `samples/distributed-app` using the integration test suite.

## User Extensions
<!-- The user can add specific rules or preferences below this line -->
