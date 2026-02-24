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
    # Use the shade jar or just the classes if possible, but shade is better for "fat jar" simulation
    # The previous plan mentioned 'target/api_guard-1.0-SNAPSHOT.jar'
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

    # Verify expected services
    # Based on the file listing:
    # caller_feign_client
    # caller_rest_template
    # caller_web_client
    # responder_mvc_annotation
    # responder_mvc_router
    # responder_webflux

    # The tool seems to use the directory name (prefixed with /) if it can't find spring application name property

    service_names = [s.get('name') for s in data]
    print(f"Found services: {service_names}")

    expected = {
        '/caller_feign_client',
        '/caller_rest_template',
        '/caller_web_client',
        '/responder_mvc_annotation',
        '/responder_mvc_router',
        '/responder_webflux'
    }
    found = set(service_names)

    missing = expected - found
    assert not missing, f"Missing expected services: {missing}"
