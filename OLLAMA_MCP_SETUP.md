# Setting up Ollama with MCP and React Client

This document provides instructions on how to set up Ollama to work with the Model Context Protocol (MCP) server provided by the API Guard project (`server.py`), and how to run the React client to interact with it.

## Prerequisites

1.  **Ollama**: Install [Ollama](https://ollama.com/) locally.
2.  **Node.js**: Install Node.js (v18+) and npm.
3.  **Python**: Install Python 3.10+ and `pip`.
4.  **Java/JavaFX**: Install JDK 21+ and JavaFX SDK as required by the `api_guard` project.
5.  **`api_guard` jar**: Build the `api_guard` jar file.

## 1. Start the MCP Server (`server.py`)

The `server.py` script uses `FastMCP` to expose the Java analyzer's tools (like `list_services`, `get_service_details`, `find_incoming_calls`).

First, set the required environment variables:

*   **Linux/macOS:**
    ```bash
    export JAVA_PATH="java"
    export JAVAFX_LIB_PATH="/path/to/javafx-sdk/lib"
    export API_GUARD_JAR_PATH="dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar"
    export PROJECTS_PATH="/path/to/microservices/directory"
    ```

*   **Windows (PowerShell):**
    ```powershell
    $env:JAVA_PATH="java"
    $env:JAVAFX_LIB_PATH="C:\path\to\javafx-sdk\lib"
    $env:API_GUARD_JAR_PATH="dependency_analyzers\java\api_guard\target\api_guard-1.0-SNAPSHOT.jar"
    $env:PROJECTS_PATH="C:\path\to\microservices\directory"
    ```

Run the server:
```bash
python server.py
```
*Note: Ensure `mcp` is installed (`pip install mcp`).*

## 2. Configure Ollama with an MCP Client

Currently, Ollama itself does not natively act as an MCP Client. To bridge Ollama with the MCP Server (`server.py`), you need an intermediary tool or client that supports both.

A common approach is to use a CLI tool like `mcp-cli` or integrate it through a framework like LangChain or LlamaIndex.

### Example: Using an MCP CLI client

1.  Install an MCP CLI (e.g., if using a node-based MCP cli tool):
    ```bash
    npm install -g @modelcontextprotocol/cli
    ```

2.  Connect the CLI to the running `server.py` and pass Ollama as the model provider:
    *Note: The exact command depends on the specific MCP client implementation. The client will handle routing tools from `server.py` to Ollama's `llama3.2` model.*

## 3. Pull the llama3.2 Model

Ensure you have the required model pulled in Ollama:

```bash
ollama run llama3.2
```
(You can type `/bye` to exit the interactive prompt).

## 4. Allow CORS in Ollama (Crucial for React Client)

By default, Ollama blocks cross-origin requests. Since the React app runs on a different port (usually 5173), you must configure Ollama to allow it.

Set the `OLLAMA_ORIGINS` environment variable before starting Ollama:

*   **Linux/macOS:**
    ```bash
    OLLAMA_ORIGINS="*" ollama serve
    ```
    *Or, add it to your service file (e.g., `systemd`) if running in the background.*

*   **Windows (Command Prompt):**
    ```cmd
    set OLLAMA_ORIGINS="*"
    ollama serve
    ```

*   **Windows (PowerShell):**
    ```powershell
    $env:OLLAMA_ORIGINS="*"
    ollama serve
    ```

## 5. Run the React Client

Navigate to the React application directory, install dependencies, and start the development server.

```bash
cd react-ollama-client
npm install
npm run dev
```

Open your browser to the URL provided by Vite (usually `http://localhost:5173`).

You can now chat with Ollama.

**Important Note on React Client and MCP:**
The basic React client currently communicates *directly* with Ollama's REST API (`/api/chat`).
If you want the React client to trigger MCP tools, the architecture needs to change:
1.  The React client sends a message to an intermediary Node.js or Python backend.
2.  The backend acts as the MCP Client, connects to `server.py`, and queries Ollama (passing the available tools).
3.  The backend returns the final response to the React client.
