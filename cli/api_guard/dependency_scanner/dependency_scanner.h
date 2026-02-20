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

#ifndef DEPENDENCY_SCANNER_H
#define DEPENDENCY_SCANNER_H
#include <boost/graph/adjacency_list.hpp>
#include <map>
#include <string>
#include <utility>

#include "../models/DataHub.h"

using Graph = boost::adjacency_list<boost::vecS, boost::vecS, boost::directedS, DataHub, Link>;
using Vertex = boost::graph_traits<Graph>::vertex_descriptor;

class DependencyScanner
{
public:
    explicit DependencyScanner(std::string java_analyzer_path, std::string javafx_path)
        : javaAnalyzerPath(std::move(java_analyzer_path)), javaFxPath(std::move(javafx_path))
    {
    }

    Graph scanDependencies(const std::string& projects_root);

private:
    Graph dhGraph;
    std::vector<DataHub> dataHubs;
    std::map<std::string, DataHub*> fqnDataHub;
    std::map<std::string, Vertex> adj;
    std::string javaAnalyzerPath;
    std::string javaFxPath;

    void addVertex(const DataHub* dh);
    void addEdge(const Consumer& cons, const Producer& prod);
};

#endif //DEPENDENCY_SCANNER_H
