import json
import logging
import os
import platform
import shutil
import subprocess
from typing import Dict, Any
from typing import List

from mcp.server.fastmcp import FastMCP

logging.basicConfig(
    level=logging.DEBUG,  # Captures all internal MCP traffic
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

# 1. Initialize the Server
mcp = FastMCP("Microservice-Graph")

# 2. Your Data (In a real app, load this from your analysis output file)
DATA = []


# --- 3. Define Tools (Functions the LLM can use) ---

@mcp.tool()
def list_services() -> List[str]:
    """Returns a list of all microservice artifact IDs in the system."""
    return [service["artifactId"] for service in DATA]


@mcp.tool()
def get_service_details(artifact_id: str) -> str:
    """Get full details (endpoints, consumers) for a specific service by its artifactId."""
    for service in DATA:
        if service["artifactId"] == artifact_id:
            return json.dumps(service, indent=2)
    return f"Service '{artifact_id}' not found."


@mcp.tool()
def find_incoming_calls(target_service: str) -> List[str]:
    """Finds which services call the target_service."""
    callers = []
    for service in DATA:
        # Check links in every service to see if they point to the target
        if "links" in service:
            for link in service["links"]:
                # The 'to' field usually looks like 'group:artifact', so we check if it contains our target
                if target_service in link["to"]:
                    callers.append(
                        f"Service '{service['artifactId']}' calls '{target_service}' on endpoint {link['producer']['url']}")
    return callers if callers else ["No incoming calls found."]


# --- 4. Define Resources (Direct file access) ---

@mcp.resource("microservices://graph")
def get_full_graph() -> str:
    """Returns the complete JSON graph of the microservice architecture."""
    return json.dumps(DATA, indent=2)


def run_executable(executable_path: str, args: list) -> Dict[str, Any]:
    """
    Runs a cross-platform executable, captures stdout, and parses JSON.

    Args:
        executable_path: Path to the binary (e.g., "./analyzer")
        args: List of arguments (e.g., ["--target", "src/"])

    Returns:
        Parsed JSON dictionary.
    """

    # 1. Handle Windows ".exe" extension automatically
    if platform.system() == "Windows" and not executable_path.endswith(".exe"):
        executable_path += ".exe"

    # 2. Check if executable exists (handles both absolute paths and PATH-resolved commands)
    resolved_path = shutil.which(executable_path)
    if resolved_path is None and not os.path.exists(executable_path):
        raise FileNotFoundError(f"Executable not found at: {executable_path}")

    # Use resolved path if available (for PATH commands like "java")
    if resolved_path:
        executable_path = resolved_path

    try:
        # 3. Run the process safely
        # capture_output=True -> Captures stdout/stderr
        # text=True -> Decodes bytes to string (handles \r\n vs \n automatically)
        result = subprocess.run(
            [executable_path] + args,
            capture_output=True,
            text=True,
            check=True  # Raises exception if exit code != 0
        )

        # 4. Parse JSON from Standard Output
        return json.loads(result.stdout)

    except subprocess.CalledProcessError as e:
        # The program ran but crashed (exit code != 0)
        raise RuntimeError(f"Executable failed (Exit {e.returncode}): {e.stderr}")

    except json.JSONDecodeError:
        # The program ran but didn't return valid JSON
        raise ValueError(f"Output was not valid JSON. Raw Output: {result.stdout}")


if __name__ == "__main__":
    java_path = os.environ.get("JAVA_PATH", "java")
    javafx_lib_path = os.environ.get("JAVAFX_LIB_PATH")
    if not javafx_lib_path:
        raise ValueError("JAVAFX_LIB_PATH environment variable is required.")

    api_guard_jar_path = os.environ.get("API_GUARD_JAR_PATH", "dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar")
    projects_path = os.environ.get("PROJECTS_PATH")
    if not projects_path:
        raise ValueError("PROJECTS_PATH environment variable is required.")

    DATA = run_executable(
        java_path,
        [
            "--module-path",
            javafx_lib_path,
            "--add-modules",
            "javafx.controls",
            "-jar",
            api_guard_jar_path,
            "--cli",
            f"--path={projects_path}"
        ]
    )
    mcp.run()
