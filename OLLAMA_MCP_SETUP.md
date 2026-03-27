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

## 2. Configure Ollama with an MCP HTTP Bridge

Ollama itself does not natively resolve MCP tools directly via its REST API. To bridge Ollama with the MCP Server (`server.py`), so that applications like React or Python scripts can access Ollama *with* tool capabilities, you must run an HTTP proxy or bridge (such as an OpenAI-compatible API bridge like `liteLLM` or a dedicated `ollama-mcp-bridge`).

This bridge listens for incoming API requests (e.g., from the React app), coordinates tool execution between the local `server.py` over SSE, and proxies generation back and forth with the underlying Ollama instance.

### Step-by-Step Bridge Setup

1.  **Install an MCP HTTP Bridge:**
    Install a bridge tool compatible with your environment. For example, if using an `ollama-mcp-bridge` or `litellm`:
    ```bash
    pip install litellm mcp
    ```

2.  **Create an MCP Client Configuration File:**
    A file named `mcp-config.json` is provided in the root directory. It tells the bridge how to connect to your `server.py` MCP server (which is configured to run via SSE on port 8001).

    ```json
    {
      "mcpServers": {
        "api-guard-server": {
          "url": "http://localhost:8001/sse"
        }
      }
    }
    ```

3.  **Start the Bridge Server:**
    Make sure your `server.py` is running in one terminal (listening on port 8001).
    In a new terminal, start your bridge so it proxies requests to Ollama and exposes an OpenAI-compatible API on port 8000.

    *(Example using litellm with MCP config)*
    ```bash
    litellm --model ollama_chat/llama3.2 --port 8000 --mcp_config mcp-config.json
    ```

    The bridge is now listening on `http://localhost:8000`. Applications should target this URL rather than hitting Ollama directly.

## 3. Pull the llama3.2 Model

Ensure you have the required model pulled in Ollama:

```bash
ollama run llama3.2
```
(You can type `/bye` to exit the interactive prompt).

## 4. Run the React Client

Navigate to the React application directory, install dependencies, and start the development server.

```bash
cd react-ollama-client
npm install
npm run dev
```

Open your browser to the URL provided by Vite (usually `http://localhost:5173`). The Vite development server is configured to proxy requests to your HTTP Bridge running on port 8000, avoiding CORS issues and correctly routing the requests.

You can now chat with Ollama, and it will be able to invoke the Java analyzer MCP tools!
