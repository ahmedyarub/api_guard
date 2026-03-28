# Setting up Ollama with MCP Bridge and React Client

This document outlines the architecture and setup instructions for connecting a React client to an Ollama model (`qwen3:0.6b`), using an MCP bridge to expose Python tools (`server.py`) to the LLM.

## Architecture Flow

1. **React Application** -> Sends a `/api/chat` request (proxied by Vite) ->
2. **MCP HTTP Bridge** (Listening on port 8000) ->
    * The bridge holds the connection and acts as an agent.
    * It connects to `server.py` via SSE (on port 8001) to discover available MCP tools.
    * It queries Ollama (`http://localhost:11434`) and handles any `tool_calls` Ollama requests autonomously.
    * Once finished, it returns the final completed chat message to the React app.

## Prerequisites

1.  **Ollama**: Install [Ollama](https://ollama.com/) locally.
2.  **ollama-mcp-bridge**: install [ollama-mcp-bridge](https://github.com/jonigl/ollama-mcp-bridge?tab=readme-ov-file#installation) locally.
3.  **Node.js**: Install Node.js (v18+) and npm.
4.  **Python**: Install Python 3.10+ and `pip` (ensure `mcp` is installed).
5.  **Java/JavaFX**: Install JDK 21+ and JavaFX SDK as required by the `api_guard` project.
6.  **`api_guard` jar**: Build the `api_guard` jar file.

## 1. Start the MCP Server (`server.py`)

The `server.py` script exposes the Java analyzer's tools over an SSE connection on port 8001.

Set the required environment variables:

*   **Linux/macOS:**
    ```bash
    export JAVA_PATH="java"
    export JAVAFX_LIB_PATH="/path/to/javafx-sdk/lib"
    export API_GUARD_JAR_PATH="dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar"
    export PROJECTS_PATH="/path/to/microservices/directory"
    ```

Run the server:
```bash
python server.py
```

## 2. Pull the required Model

Ensure you have the required model pulled in Ollama:

```bash
ollama run qwen3:0.6b
```
(You can type `/bye` to exit the interactive prompt).

## 3. Start the MCP HTTP Bridge

The HTTP bridge sits between the frontend, Ollama, and `server.py`. Ensure your bridge (`ollama-mcp-bridge` configured with the MCP context) is running and listening on port 8000, and is configured to talk to `http://localhost:8001/sse`.

```bash
ollama-mcp-bridge --config mcp-config.json
```

## 4. Run the React Client

Navigate to the React application directory, install dependencies, and start the development server.

```bash
cd react-ollama-client
npm install
npm run dev
```

Open your browser to the URL provided by Vite (usually `http://localhost:5173`).

The Vite development server is configured to proxy all `/api` requests to `http://127.0.0.1:8000`, routing your chat messages straight into the bridge without encountering CORS issues.