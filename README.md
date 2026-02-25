![header.png](header.png)
# API Guard - Community Edition

Cross-domain static analysis for Java microservices. API Guard discovers REST endpoints and client calls across Spring Boot services, links them into a dependency graph, and enables [CodeQL](https://codeql.github.com/) to analyze vulnerabilities that span service boundaries.

## How It Works

1. **Scan** - The Java analyzer parses Spring Boot projects to extract REST producers (`@RestController`, `RouterFunction`) and consumers (`FeignClient`, `RestTemplate`, `WebClient`).
2. **Link** - Consumer-to-producer relationships are matched by URL path, building a directed dependency graph.
3. **Adapt** - CodeQL TRAP files are modified to replace client stubs with actual controller implementations, enabling cross-service dataflow analysis.
4. **Analyze** - CodeQL runs with the enriched databases, finding vulnerabilities that would be invisible to single-service analysis.

## Project Structure

```
├── cli/api_guard/           # C++ CLI orchestrator
│   ├── main.cpp             # Entry point: scans, builds CodeQL DBs, adapts TRAP files
│   ├── dependency_scanner/  # Invokes the Java analyzer and builds the dependency graph
│   ├── process_handler/     # Cross-platform process execution
│   └── models/              # DataHub, Link, Producer, Consumer
│
└── dependency_analyzers/
    └── java/api_guard/      # Java analyzer (Maven + JavaFX)
        └── src/main/java/org/mindpower/api_guard/
            ├── service/     # AnalysisService, EndpointExtractor, RestClientExtractor
            └── models/      # DataHub, Link, Endpoint, RestClient, Producer, Consumer
```

## Prerequisites

- **C++ 23** compiler (GCC 13+, Clang 17+, or MSVC 2022)
- **CMake** 3.28.3+
- **vcpkg** (for Boost, cxxopts, nlohmann-json)
- **Java 25** with JavaFX
- **Maven** 3.9+
- **CodeQL CLI** on PATH

## Building

### Java Analyzer

```bash
cd dependency_analyzers/java/api_guard
mvn package
```

### C++ CLI

```bash
cd cli/api_guard
cmake -B build -S . -DCMAKE_TOOLCHAIN_FILE=$VCPKG_ROOT/scripts/buildsystems/vcpkg.cmake
cmake --build build
```

### Packaging

```bash
cd cli/api_guard/build
cpack
```

This produces a ZIP containing the `api_guard` executable and the Java analyzer JAR.

## Usage

```bash
api_guard \
  --projects_path=/path/to/microservices \
  --dbs_path=/path/to/codeql-dbs \
  --javafx_path=/path/to/javafx-sdk
```

| Flag | Description |
|------|-------------|
| `--projects_path` | Root directory containing Spring Boot projects |
| `--dbs_path` | Output directory for CodeQL databases |
| `--javafx_path` | Path to the JavaFX SDK |
| `--java_analyzer_path` | *(Optional)* Path to the Java analyzer JAR |

The Java analyzer can also run standalone with a GUI:

```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls \
  -jar java_analyser.jar
```

Or in CLI mode:

```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls \
  -jar java_analyser.jar --cli --path=/path/to/microservices
```

## License

Licensed under the [Apache License 2.0](cli/api_guard/LICENSE).

Copyright 2026 Ahmed Yarub Hani Al Nuaimi.
