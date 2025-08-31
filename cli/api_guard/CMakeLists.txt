cmake_minimum_required(VERSION 3.28.3)
project(CodeQLTool)

set(CMAKE_CXX_STANDARD 23)

find_package(Boost REQUIRED NO_MODULE COMPONENTS dll graph process)
find_package(cxxopts CONFIG REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)

add_executable(api_guard
        main.cpp
        dependency_scanner/dependency_scanner.cpp
        process_handler/process_handler.cpp)

target_link_libraries(api_guard PRIVATE Boost::dll)
target_link_libraries(api_guard PRIVATE Boost::graph)
target_link_libraries(api_guard PRIVATE Boost::process)
target_link_libraries(api_guard PRIVATE cxxopts::cxxopts)
target_link_libraries(api_guard PRIVATE nlohmann_json::nlohmann_json)

target_compile_definitions(api_guard PRIVATE WIN32_LEAN_AND_MEAN)
target_compile_definitions(api_guard PRIVATE _WIN32_WINNT=0x0601)

add_custom_command(
        OUTPUT "${CMAKE_SOURCE_DIR}/../../dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar"
        COMMAND mvn -f "${CMAKE_SOURCE_DIR}/../../dependency_analyzers/java/api_guard/pom.xml" package
        WORKING_DIRECTORY "${CMAKE_SOURCE_DIR}/../../dependency_analyzers/java/api_guard"
        COMMENT "Building Java Analyzer"
)

add_custom_target(build_analyzer_jar ALL
        DEPENDS "${CMAKE_SOURCE_DIR}/../../dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar"
)

install(FILES
        "${CMAKE_SOURCE_DIR}/../../dependency_analyzers/java/api_guard/target/api_guard-1.0-SNAPSHOT.jar"
        DESTINATION analysers/java
        RENAME java_analyser.jar
)

install(TARGETS api_guard DESTINATION .)

set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)
set(X_VCPKG_APPLOCAL_DEPS_INSTALL ON)

set(CPACK_PACKAGE_NAME "api_guard")
set(CPACK_PACKAGE_VENDOR "Mind Power")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "API Static Analyzer")
set(CPACK_PACKAGE_VERSION "0.0.1")
set(CPACK_PACKAGE_CONTACT "ahmedyarubhani@gmail.com")

set(CPACK_GENERATOR "ZIP")

include(CPack)