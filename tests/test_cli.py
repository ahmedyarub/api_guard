import pytest
import os
import subprocess
import platform

def test_integration_cli():
    # Setup paths
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    api_guard_dir = os.path.join(repo_root, 'dependency_analyzers', 'java', 'api_guard')

    # Corrected path: point to the 'microservices' directory inside the submodule
    samples_dir = os.path.join(repo_root, 'samples', 'distributed-app', 'microservices')

    # Initialize submodule
    print("Initializing submodule...")
    subprocess.run(
        ["git", "submodule", "update", "--init", "--recursive"],
        cwd=repo_root,
        check=True
    )

    # Determine Maven wrapper script
    if platform.system() == "Windows":
        mvnw_cmd = os.path.join(api_guard_dir, "mvnw.cmd")
    else:
        mvnw_cmd = os.path.join(api_guard_dir, "mvnw")

    # Build Command
    build_cmd = [
        mvnw_cmd, "clean", "package",
        "-DskipTests",
        "-Dmaven.compiler.source=21",
        "-Dmaven.compiler.target=21",
        "-Djavafx.version=21"
    ]

    print(f"Building in {api_guard_dir} with command: {build_cmd}")
    build_process = subprocess.run(
        build_cmd,
        cwd=api_guard_dir,
        capture_output=True,
        text=True
    )

    if build_process.returncode != 0:
        print("Build STDOUT:", build_process.stdout)
        print("Build STDERR:", build_process.stderr)
        pytest.fail("Maven build failed.")

    # Run Command
    jar_path = os.path.join(api_guard_dir, 'target', 'api_guard-1.0-SNAPSHOT.jar')

    # We must use the Launcher to avoid JavaFX module issues on the classpath
    run_cmd = [
        "java", "-cp", jar_path,
        "org.mindpower.api_guard.Launcher",
        "--cli",
        f"--path={samples_dir}"
    ]

    print(f"Running: {' '.join(run_cmd)}")
    run_process = subprocess.run(
        run_cmd,
        capture_output=True,
        text=True
    )

    if run_process.returncode != 0:
        print("Run STDOUT:", run_process.stdout)
        print("Run STDERR:", run_process.stderr)
        pytest.fail("CLI execution failed.")

    import json
    try:
        data = json.loads(run_process.stdout)
    except json.JSONDecodeError:
        print("Invalid JSON Output:", run_process.stdout)
        pytest.fail("Output is not valid JSON")

    # Helper to find service by artifactId or name
    def get_service(services, name_or_artifact):
        for s in services:
            if s.get('artifactId') == name_or_artifact or s.get('name') == name_or_artifact or s.get('name') == f"/{name_or_artifact}":
                return s
        return None

    # Verify expected services and links based on the user provided JSON

    # 1. caller_feign_client -> responder_mvc_annotation
    caller_feign = get_service(data, "caller_feign_client")
    assert caller_feign, "caller_feign_client not found"

    links = caller_feign.get("links", [])
    found_link = False
    for link in links:
        if "responder_mvc_annotation" in link.get("to", ""):
             # Verify details
             consumer_url = link.get("consumer", {}).get("url")
             producer_url = link.get("producer", {}).get("url")
             if consumer_url == "/responder_mvc_annotation/annotation/lower/{param}" and \
                producer_url == "/annotation/lower/{param}":
                 found_link = True
                 break
    assert found_link, "Link from caller_feign_client to responder_mvc_annotation not found or incorrect"

    # 2. caller_rest_template -> responder_mvc_router
    caller_rest = get_service(data, "caller_rest_template")
    assert caller_rest, "caller_rest_template not found"

    links = caller_rest.get("links", [])
    found_link = False
    for link in links:
        if "responder_mvc_router" in link.get("to", ""):
             # Verify details
             consumer_url = link.get("consumer", {}).get("url")
             producer_url = link.get("producer", {}).get("url")
             if consumer_url == "/responder_mvc_router/router/lower/{param}" and \
                producer_url == "/router/lower/{param}":
                 found_link = True
                 break
    assert found_link, "Link from caller_rest_template to responder_mvc_router not found or incorrect"

    # 3. caller_web_client -> responder_webflux
    caller_web = get_service(data, "caller_web_client")
    assert caller_web, "caller_web_client not found"

    links = caller_web.get("links", [])
    found_link = False
    for link in links:
        if "responder_webflux" in link.get("to", ""):
             # Verify details
             consumer_url = link.get("consumer", {}).get("url")
             producer_url = link.get("producer", {}).get("url")
             if consumer_url == "/responder_webflux/annotation/lower/{param}" and \
                producer_url == "/annotation/lower/{param}":
                 found_link = True
                 break
    assert found_link, "Link from caller_web_client to responder_webflux not found or incorrect"

    print("All expected services and links verified successfully.")
