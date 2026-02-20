// Copyright 2026 Ahmed Yarub Hani Al Nuaimi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <iostream>
#include <boost/dll.hpp>
#include <boost/process.hpp>
#include <boost/graph/topological_sort.hpp>
#include "cxxopts.hpp"
#include <deque>
#include <fstream>

#include "dependency_scanner/dependency_scanner.h"
#include "process_handler/process_handler.h"

#ifdef _WIN32
#define SCRIPT_EXT ".cmd"
#else
#define SCRIPT_EXT ".sh"
#endif

namespace fs = std::filesystem;

void adaptTrapFiles(const Graph& g, const Vertex& cons_dep, const std::string& dbs_root);

int main(const int argc, char* argv[])
{
    cxxopts::Options options("API Guard", "Enabling cross-domain static analysis");

    options.add_options()
        ("projects_path", "Root path of projects", cxxopts::value<std::string>())
        ("dbs_path", "Root path of databases", cxxopts::value<std::string>())
        ("javafx_path", "Root path of JavaFX", cxxopts::value<std::string>())
        ("java_analyzer_path", "Path to the .jar file used to analyze the dependencies of Java projects",
         cxxopts::value<std::string>())
        ("h,help", "Print usage");

    const auto argsResult = options.parse(argc, argv);

    if (argsResult.count("help") ||
        !argsResult.count("projects_path") ||
        !argsResult.count("dbs_path") ||
        !argsResult.count("javafx_path"))
    {
        std::cout << options.help() << std::endl;

        return 1;
    }

    const auto projects_root = argsResult["projects_path"].as<std::string>();
    const auto dbs_root = argsResult["dbs_path"].as<std::string>();
    const auto javafx_path = argsResult["javafx_path"].as<std::string>();
    std::string java_analyzer_path;

    if (argsResult.count("java_analyzer_path"))
    {
        java_analyzer_path = argsResult["java_analyzer_path"].as<std::string>();
    }
    else
    {
        java_analyzer_path =
            boost::dll::program_location().parent_path().generic_string() + "/analysers/java/java_analyser.jar";
    }

    auto codeqlExePath = getExecutablePath("codeql");
    if (codeqlExePath.empty())
    {
        std::cerr << "Error: 'codeql' executable not found on PATH." << std::endl;
        return 1;
    }
    auto codeqlRoot = codeqlExePath.parent_path();

    DependencyScanner scanner(java_analyzer_path, javafx_path);
    std::cout << "Scanning root folder for projects" << std::endl;
    auto g = scanner.scanDependencies(projects_root);

    if (fs::exists(dbs_root))
    {
        std::cout << std::endl << "Cleaning up databases folder" << std::endl;
        fs::remove_all(dbs_root);
    }

    fs::create_directory(dbs_root);

    std::deque<Vertex> deps;
    boost::topological_sort(g, std::front_inserter(deps));
    std::string verbosityFlag = "--verbosity=warnings";

    for (const auto dep : deps)
    {
        const auto& dh = g[dep];

        std::cout << std::endl << "Creating database for " << dh.getFqn() << std::endl;

        std::string dbDir = dbs_root + "/" + dh.artifactId;

        runProcess(codeqlExePath, {
                       "database", "init",
                       "-s=" + dh.rootFolder,
                       "-l=java",
                       "--overwrite",
                       verbosityFlag,
                       "--",
                       dbDir
                   }, {});

        std::cout << "Parsing code" << std::endl;

        runProcess(codeqlExePath, {
                       "database", "trace-command",
                       "--working-dir=" + dh.rootFolder, "--index-traceless-dbs", "--no-db-cluster",
                       verbosityFlag, "--", dbDir,
                       codeqlRoot.generic_string() + "/java/tools/autobuild" + SCRIPT_EXT
                   },
                   {"CODEQL_EXTRACTOR_JAVA_OPTION_TRAP_COMPRESSION=NONE"});

        std::cout << "Adapting Trap files" << std::endl;
        adaptTrapFiles(g, dep, dbs_root);

        std::cout << "Finalizing database" << std::endl;
        runProcess(codeqlExePath, {
                       "database", "finalize",
                       "--no-cleanup",
                       "--no-db-cluster",
                       verbosityFlag,
                       "--",
                       dbs_root + "/" + dh.artifactId
                   },
                   {"CODEQL_EXTRACTOR_JAVA_OPTION_TRAP_COMPRESSION=NONE"});

        if (const std::string result_file = dbs_root + "/" + dh.artifactId + "/codeql_result.sarif";
            fs::exists(result_file))
        {
            fs::remove(result_file);
        }

        std::cout << "Analyzing project" << std::endl;
        runProcess(codeqlExePath, {
                       "database", "analyze",
                       dbs_root + "/" + dh.artifactId,
                       "--format=sarif-latest",
                       "--output=" + dh.rootFolder + "/codeql_result.sarif",
                       "--rerun",
                       "--threads=0",
                       "java-lgtm-full",
                       "--no-sarif-minify",
                       "--sarif-add-snippets",
                       verbosityFlag,
                   }, {});
    }

    return 0;
}

void adaptTrapFiles(const Graph& g, const Vertex& cons_dep, const std::string& dbs_root)
{
    auto dh = g[cons_dep];
    std::string consDbRoot = dbs_root + "/" + dh.artifactId;
    std::string normalizedConsRoot = dh.rootFolder;
    std::ranges::replace(normalizedConsRoot, ':', '_');

    for (auto scanRoot = std::format("{}/trap/java/{}", consDbRoot, normalizedConsRoot); const auto& entry :
         fs::recursive_directory_iterator(scanRoot))
    {
        if (entry.is_regular_file() && entry.path().extension() == ".trap")
        {
            const std::string trap_file = entry.path().string();
            std::cout << "Processing Trap file: " << trap_file << '\n';

            std::ifstream in(trap_file);
            std::string contents((std::istreambuf_iterator(in)), std::istreambuf_iterator<char>());
            in.close();

            for (const auto& [from, to] : dh.links)
            {
                size_t pos = 0;
                auto consumerClass = from.clientClass;
                auto producerClass = to.controller;

                while ((pos = contents.find(consumerClass, pos)) != std::string::npos)
                {
                    std::cout << "Replacing " << consumerClass << " with " << producerClass << '\n';
                    contents.replace(pos, consumerClass.length(), producerClass);
                    pos += producerClass.length();
                }
            }

            std::ofstream out(trap_file);
            out << contents;
            out.close();
        }
    }

    boost::graph_traits<Graph>::edge_iterator ei, ei_end;
    for (boost::tie(ei, ei_end) = boost::edges(g); ei != ei_end; ++ei)
    {
        if (boost::target(*ei, g) == cons_dep)
        {
            auto prod_dep = g[boost::source(*ei, g)];
            std::string prodDbRoot = dbs_root + "/" + prod_dep.artifactId;
            std::string normalizedProdRoot = prod_dep.rootFolder;
            std::ranges::replace(normalizedProdRoot, ':', '_');

            fs::copy(std::format("{}/trap/java/{}", prodDbRoot, normalizedProdRoot),
                     std::format("{}/trap/java/{}", consDbRoot, normalizedProdRoot),
                     fs::copy_options::recursive);

            std::string prodClassesFolder = prod_dep.getFqn();
            std::ranges::replace(prodClassesFolder, '.', '/');

            fs::copy(std::format("{}/trap/java/classes/{}", prodDbRoot, prodClassesFolder),
                     std::format("{}/trap/java/classes/{}", consDbRoot, prodClassesFolder),
                     fs::copy_options::recursive);
        }
    }
}
